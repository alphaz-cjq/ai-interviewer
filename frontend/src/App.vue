<template>
  <router-view />
</template>

<style>
:root {
  --bg-start: #F3E8FF;
  --bg-mid: #E8F4FD;
  --bg-end: #F8E8EE;
  --primary: #9B7EC4;
  --primary-hover: #8B6EB4;
  --primary-light: #F3E8FF;
  --accent-blue: #7EB8E0;
  --accent-mint: #7EC4A8;
  --accent-pink: #D48BB8;
  --danger: #E88B8B;
  --danger-hover: #D47A7A;
  --success: #7EC494;
  --warning: #E0B87E;
  --text: #4A4A5A;
  --text-light: #8B8B9B;
  --text-lighter: #B0B0C0;
  --card-bg: #FFFFFF;
  --border: #EDE8F2;
  --shadow-sm: 0 1px 4px rgba(155, 126, 196, 0.06);
  --shadow-md: 0 4px 16px rgba(155, 126, 196, 0.10);
  --shadow-lg: 0 8px 32px rgba(155, 126, 196, 0.14);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background: linear-gradient(135deg, var(--bg-start) 0%, var(--bg-mid) 50%, var(--bg-end) 100%);
  min-height: 100vh;
  color: var(--text);
}

::selection {
  background: var(--primary-light);
  color: var(--primary);
}

::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover { background: var(--text-lighter); }

input, textarea, select, button {
  font-family: inherit;
}
</style>
