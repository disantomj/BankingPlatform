import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Primary brand colors
        primary: {
          DEFAULT: '#08A4BD',
          50: '#E6F7F9',
          100: '#CCF0F3',
          200: '#99E1E7',
          300: '#66D2DB',
          400: '#33C3CF',
          500: '#08A4BD',
          600: '#068397',
          700: '#056271',
          800: '#03414B',
          900: '#022025',
        },
        // Secondary blue
        secondary: {
          DEFAULT: '#4BA3C3',
          50: '#EDF6F9',
          100: '#DBEDF3',
          200: '#B7DBE7',
          300: '#93C9DB',
          400: '#6FB6CF',
          500: '#4BA3C3',
          600: '#3C829C',
          700: '#2D6275',
          800: '#1E414E',
          900: '#0F2127',
        },
        // Neutral colors
        neutral: {
          DEFAULT: '#6E6A6F',
          50: '#F5F4F5',
          100: '#EBEAEB',
          200: '#D7D5D7',
          300: '#C3C0C3',
          400: '#AFABAF',
          500: '#6E6A6F',
          600: '#585559',
          700: '#424043',
          800: '#2C2A2D',
          900: '#161516',
        },
        // Warm accent
        accent: {
          DEFAULT: '#D3B99F',
          50: '#F8F5F2',
          100: '#F1EBE5',
          200: '#E3D7CB',
          300: '#D5C3B1',
          400: '#C7AF97',
          500: '#D3B99F',
          600: '#A6947F',
          700: '#7A6F5F',
          800: '#4D4A3F',
          900: '#21251F',
        },
        // Dark theme
        dark: {
          DEFAULT: '#0A0F0D',
          50: '#E6E7E6',
          100: '#CDCFCD',
          200: '#9B9F9B',
          300: '#696F69',
          400: '#373F37',
          500: '#0A0F0D',
          600: '#080C0A',
          700: '#060908',
          800: '#040605',
          900: '#020303',
        },
        // Status colors using your palette
        success: '#4BA3C3',
        warning: '#D3B99F',
        error: '#C33A3A',
        info: '#08A4BD',
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-conic':
          'conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))',
        'gradient-primary': 'linear-gradient(135deg, #08A4BD 0%, #4BA3C3 100%)',
        'gradient-dark': 'linear-gradient(135deg, #0A0F0D 0%, #6E6A6F 100%)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'card': '0 4px 6px -1px rgba(8, 164, 189, 0.1), 0 2px 4px -1px rgba(8, 164, 189, 0.06)',
        'card-hover': '0 10px 15px -3px rgba(8, 164, 189, 0.1), 0 4px 6px -2px rgba(8, 164, 189, 0.05)',
      },
    },
  },
  plugins: [],
}
export default config