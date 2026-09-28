package com.cjq.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.mapper.ResumeMapper;
import com.cjq.pojo.PO.Resume;
import com.cjq.pojo.VO.DocumentVO;
import com.cjq.service.RAGService;
import com.cjq.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    //注入mapper层
    private final ResumeMapper resumeMapper;
    //注入RAG服务层
    private final RAGService ragService;
    //保底上传目录
    @Value("${file.upload.base-dir:./uploads/resumes/}")
    private String uploadBaseDir;
    //允许上传的文件类型
    private static final List<String>ALLOWED_TYPES=List.of("application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword", "text/plain");

/*
* 简历上传解析
* */
    @Override
    public String upload(MultipartFile file, Long userId) {

        //1.空文件校验
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }

        //2.文件类型校验（防止上传exe等恶意文件）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的文件格式，仅支持PDF、Word、TXT");
        }

        //3.文件大小限制（10MB）
        if (file.getSize()>10*1024*1024){
            throw new BusinessException("文件大小不能超过10MB");
        }


      try {
          //4.保存文件到本地
          //4.1生成新文件名
          String originalFilename = file.getOriginalFilename();//获取源文件名字
          if (originalFilename == null || originalFilename.isEmpty()) {
              originalFilename = "unknown.tmp";
          }
          String suffix = "";
          if (originalFilename.contains(".")) {
              suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
          }//获取文件后缀
          String fileName = UUID.randomUUID() + suffix;//生成新文件名

          //4.2定义存储路径并创建目录
          File destDir = new File(uploadBaseDir);//定义文件存储目录
          if (!destDir.exists()) {
              destDir.mkdirs();//会创建整个路径中所有不存在的父级目录
          }
          //4.3保存文件
          File destFile = new File(destDir, fileName);//定义文件路径,直接传父目录和文件名
          file.transferTo(destFile);//将传来的文件保存到指定路径

          //5.使用Tika解析文档内容
          Tika tika=new Tika();
          String parsedtext = tika.parseToString(destFile);//将保存的文件解析为字符串

          //6.截取预览
          String preview = parsedtext.length() > 200 ? parsedtext.substring(0, 200) : parsedtext;

          //7.存入数据库
          Resume resume=new Resume();
          resume.setUserId(userId); // 设置用户ID
          resume.setName(originalFilename);// 设置文件名
          resume.setFilePath(destFile.getAbsolutePath());//设置文件路径
          resume.setFileType(contentType);//设置文件类型
          resume.setParsedText(parsedtext);//设置解析后的完整文本

          resumeMapper.insert(resume);
          //----------------测试
          // 8. 文本切分（为阶段4的向量化做准备）
          List<String> chunks = chunkText(parsedtext, 500, 80);
          log.info("简历解析完成，共切分成 {} 个文本块", chunks.size());

          // 9. 调用RAG服务，把切块向量化并存入Chroma
          //    注意：这里传入的是刚插入数据库的 resume 的 ID
          ragService.indexResume(resume.getId(), chunks);
          //10.返回预览文本（这才是真正的返回值）
          return preview;
      } catch (Exception e){
          log.error("简历解析失败",e);
          throw new BusinessException("文档解析失败："+e.getMessage());
      }
    }

    /**
     * T - Transform（转换）：从长文本到语义向量（最核心的一步）
     * 1.将长文本切成多个语义块,便于AI理解和检索
     * 2.（主厨）负责统筹调度
     * @param text 完整的简历文本
     * @param chunkSize 每块目标大小（字符数）
     * @param overlap 重叠区域大小（防止切断关键信息）
     * @return 切分后的文本列表
     */
    private List<String>chunkText(String text,int chunkSize,int overlap){
        //判断文本是否为空
        if (text == null || text.isEmpty()){
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();//用于存储切分后的文本块（成本区）

        //1.先按换行(段落)符分割（粗粉）
        String[] paragraphs = text.split("\\r?\\n");
        StringBuilder currentChunk=new StringBuilder();//临时托盘，存储切分后的文本块
        //2.遍历每个段落
        for (String para : paragraphs) {
            //2.1如果当前段落本身就超长（比如没有换行的纯文本），再按句号切
            //（特殊处理）
            if (para.length()>chunkSize){
                String[] sentences = para.split("[。！？;]");
                //2.2遍历每个句子
                for (String sentence : sentences) {
                    //（派发任务）把拆出的每一小段交给（帮厨）处理
                    processChunk(sentence.trim(), currentChunk, chunks, chunkSize, overlap);
                    //sentence.trim(), 就是segment
                }
            }else {
                //没有超过长度就按正常段落直接处理
                processChunk(para.trim(), currentChunk, chunks, chunkSize, overlap);
            }
        }
        //3.把临时存的块存入最终结果
        if (!currentChunk.isEmpty()){
            chunks.add(currentChunk.toString());
        }
        return chunks;
    }

    /*
    * 1.（帮厨）负责装盘，返回简历列表
    * 2.负责把零散的 segment 拼装成最终的 chunk，考虑重叠区域（防止切断关键信息）
    *
    * @param segment 拆出的每一小段
    * @param currentChunk 当前块
    * @param chunks 存储所有块的列表
    * @param chunkSize 每块目标大小（字符数）
    * @param overlap 重叠区域大小（防止切断关键信息）
    * */
    private void processChunk(String segment, StringBuilder currentChunk,
                              List<String> chunks, int chunkSize, int overlap){

        if (segment.isEmpty()) return;//如果段落为空，则忽略

        //如果当前块加上新段落后超过限制
        if (currentChunk.length() + segment.length() > chunkSize && currentChunk.length() > 0) {
            //超限制，把临时托盘的块存进成品
            chunks.add(currentChunk.toString());

            //保留最后overlap个字符作为重叠（保证上下文连贯）
            String overlapText = currentChunk.substring(Math.max(0, currentChunk.length() - overlap));

            //重置当前块，从重叠部分开始
            currentChunk.setLength(0);
            currentChunk.append(overlapText);
        }
        // 添加新段落到当前块
        if (currentChunk.length() > 0) {
            currentChunk.append("\n"); // 段落间加换行
        }
        currentChunk.append(segment);//添加新段落到临时托盘
    }


    /*
    * 简历列表
    * */
    @Override
    public List<DocumentVO> listByUserId(Long userId) {
        //1.查询该用户的所有简历
        List<Resume> resumes = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .orderByDesc(Resume::getCreateTime)
        );
        //2.转换成VO（只返回给前端需要的数据）
        List<DocumentVO> voList = resumes.stream()
                .map(resume -> {
                    String parsedText = resume.getParsedText();
                    String preview = "";
                    if (parsedText != null) {
                        preview = parsedText.length() > 200
                                ? parsedText.substring(0, 200) + "..."
                                : parsedText;
                    }
                    return DocumentVO.builder()
                        .id(resume.getId())
                        .name(resume.getName())
                        .fileType(resume.getFileType())
                        .preview(preview)
                        .createTime(resume.getCreateTime() != null
                                ? resume.getCreateTime().toString() : "")
                        .build();
                })
                .collect(Collectors.toList());
        return voList;
    }

    /*
    * 获取完整文本
    * */
    @Override
    public String getParsedText(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume==null){
            throw new BusinessException("简历不存在");
        }
        return resume.getParsedText();
    }

    /*
    * 删除简历
    * */
    @Override
    public void delete(Long resumeId, Long userId) {
        //1.查出该用户简历
        Resume resume=resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
        );
        if (resume==null){
            throw new BusinessException("简历不存在或无权删除");
        }

        //2.删除磁盘文件
        try {
            File file = new File(resume.getFilePath());
            if (file.exists()) {
                file.delete();
                log.info("已删除磁盘文件:{}", resume.getFilePath());
            }
        }catch (Exception e){
            log.warn("删除磁盘文件时出错:{},但数据库记录已删除", resume.getFilePath(), e);
        }

        //3.删除数据库记录
        resumeMapper.deleteById(resumeId);

        //4.删除向量库中的简历向量（失败不影响主流程，仅记录日志）
        try {
            ragService.deleteResumeByResumeId(resumeId);
        } catch (Exception e) {
            log.error("删除简历 {} 的向量数据失败，请手动清理 Chroma", resumeId, e);
        }

        log.info("简历删除成功：resumeId={},userId={}",resumeId,userId);
    }

    @Override
    @Transactional
    public void renameResume(Long resumeId, Long userId, String newName) {
        // 1. 校验新名称
        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 2. 查询简历，并校验是否属于该用户（防止越权修改他人简历）
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
        );
        if (resume == null) {
            throw new BusinessException("简历不存在或无权限修改");
        }

        // 3. 更新名称
        resume.setName(newName.trim());
        resumeMapper.updateById(resume);
        log.info("简历重命名成功：id={}, newName={}", resumeId, newName);
    }
}
