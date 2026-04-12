/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      typography: {
        DEFAULT: {
          css: {
            // 显式指定列表样式，避免与 Semi Design 等全局样式冲突，确保圆点与序号可见
            'ul': { listStyleType: 'disc' },
            'ol': { listStyleType: 'decimal' },
            'ul ul, ol ul': { listStyleType: 'circle' },
            'ul ul ul, ol ul ul, ol ol ul, ul ol ul': { listStyleType: 'square' },
            'li': { marginTop: '0.25em', marginBottom: '0.25em' },
          },
        },
      },
    },
  },
  plugins: [require('@tailwindcss/typography')],
};
