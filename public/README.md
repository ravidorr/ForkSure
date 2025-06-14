# Public Web Assets

This directory contains static assets for web deployment.

## 📁 Structure

```
public/
├── images/
    ├── apple-touch-icon.png
    ├── favicon-16x16.png
    └── favicon-32x32.png
```

## 🌐 Web Icons

- `apple-touch-icon.png` - Apple touch icon for iOS Safari (180x180px)
- `favicon-16x16.png` - Small favicon for browser tabs (16x16px)
- `favicon-32x32.png` - Standard favicon for browser tabs (32x32px)

## 🔗 Usage

These files are referenced in `index.html` for the privacy policy page:

```html
<link rel="icon" type="image/png" sizes="32x32" href="public/images/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="16x16" href="public/images/favicon-16x16.png">
<link rel="apple-touch-icon" href="public/images/apple-touch-icon.png">
<link rel="shortcut icon" href="public/images/favicon-32x32.png">
```

## 🎨 Design

All icons feature the ForkSure cupcake logo with consistent branding and colors matching the main app design. 