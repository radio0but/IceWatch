// =====================
// ✅ tinymce.js (version Community 100% gratuite via CDN ou self-hosted)
// =====================
export function setupTinyMCE(selector = '#page-html') {
  if (window.tinymce) {
    tinymce.init({
      selector, // ← utilise le paramètre ici
      license_key: 'gpl', // Pour forcer le mode Community
      height: 400,
      menubar: false,
      plugins: 'link image code lists preview fullscreen table',
      toolbar:
        'undo redo | formatselect fontsizeselect | bold italic underline forecolor backcolor | ' +
        'alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | ' +
        'blockquote code table | link image | preview fullscreen',
      style_formats: [
        { title: 'Titre principal (h1)', block: 'h1' },
        { title: 'Titre section (h2)', block: 'h2' },
        { title: 'Sous-section (h3)', block: 'h3' },
        { title: 'Paragraphe', block: 'p' },
        { title: 'Citation', block: 'blockquote' },
        { title: 'Code', block: 'pre' }
      ],
      skin: 'oxide-dark',
      content_css: 'dark',
      branding: false,
      elementpath: false,
      contextmenu: false,
      content_style: `
        body {
          font-family: Inconsolata, monospace;
          font-size: 16px;
          color: #f1f1f1;
          background-color:rgba(0, 0, 0, 0.93) ;
        }
        h1, h2, h3 {
          font-weight: bold;
          color: #ff5252;
        }
        blockquote {
          border-left: 3px solid #666;
          margin-left: 0;
          padding-left: 1rem;
          font-style: italic;
          color: #ccc;
        }
        pre {
          background: #222;
          padding: 1rem;
          font-family: monospace;
          overflow-x: auto;
          border-radius: 5px;
          color: #ddd;
        }
        .page-wrapper {
          background-color: rgba(0, 0, 0, 0.8);
          padding: 1rem;
          border-radius: 8px;
          color: #eee;
        }
      `,
      setup: (editor) => {
        editor.on('change', () => {
          editor.save(); // Synchronise avec le <textarea>
        });
      }
    });
  } else {
    console.warn("TinyMCE n'est pas chargé !");
  }
}

export function setupDualEditor() {
  setupTinyMCE('#audio-info');
  setupTinyMCE('#video-info');
}
export function destroyTinyMCE(selector) {
    const inst = tinymce.get(selector.replace("#", ""));
    if (inst) {
        inst.remove();
    }
}