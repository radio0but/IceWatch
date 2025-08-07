// =======================
// ✅ appearanceEditor.js
// =======================
import { setupTinyMCE } from "./tinymce.js";

const FIELD_CONFIG = [
  { key: 'appearance.radio-title', label: 'Titre de la radio', type: 'tinymce' },
  { key: 'appearance.welcome-message', label: 'Message de bienvenue', type: 'tinymce' },
  { key: 'appearance.login-title', label: 'Titre de la page de connexion', type: 'input' },
  { key: 'appearance.custom-css', label: 'CSS personnalisé', type: 'textarea' },
  { key: 'appearance.custom-html', label: 'Footer HTML', type: 'tinymce' },
  { key: 'appearance.radio-plain-title', label: 'Titre simple (fallback)', type: 'input' },
  { key: 'appearance.favicon', label: 'Nom du favicon', type: 'input' },
];

export function initAppearanceEditor() {
  const textarea = document.getElementById('properties-editor');
  if (!textarea) return;

  const blob = textarea.value;
  const props = parseProperties(blob);

  const container = document.createElement('div');
  container.id = 'appearance-form';
  container.style.marginTop = '2rem';

  for (const { key, label, type } of FIELD_CONFIG) {
    const value = props[key] || '';
    const wrapper = document.createElement('div');
    wrapper.style.marginBottom = '1.5rem';

    const labelEl = document.createElement('label');
    labelEl.textContent = label;
    labelEl.style.display = 'block';
    labelEl.style.marginBottom = '0.5rem';

    let inputEl;
    const id = key.replace(/\./g, '-'); // appearance.radio-title → appearance-radio-title

    if (type === 'input') {
      inputEl = document.createElement('input');
      inputEl.type = 'text';
      inputEl.value = value;
    } else {
      inputEl = document.createElement('textarea');
      inputEl.rows = 4;
      inputEl.value = value;
    }

    inputEl.id = id;
    inputEl.classList.add('form-field');
    inputEl.dataset.key = key;
    inputEl.style.width = '100%';

    wrapper.appendChild(labelEl);
    wrapper.appendChild(inputEl);
    container.appendChild(wrapper);
  }

  const generateBtn = document.createElement('button');
  generateBtn.textContent = '🔁 Générer le fichier .look à partir des champs';
  generateBtn.className = 'danger';
  generateBtn.style.marginTop = '1rem';
  generateBtn.onclick = () => {
    const newProps = {};
    const fields = container.querySelectorAll('.form-field');

    for (const field of fields) {
      if (window.tinymce) {
        const editor = tinymce.get(field.id);
        if (editor) {
          editor.save(); // 🔄 Sync TinyMCE → textarea
        }
      }
      newProps[field.dataset.key] = field.value;
    }

    textarea.value = generateBlob(newProps);
    alert("✅ Configuration régénérée.\nCliquez sur 💾 Appliquer pour sauvegarder.");
  };

  container.appendChild(generateBtn);
  textarea.parentNode.insertBefore(container, textarea);

  // Initialise TinyMCE pour tous les champs marqués
  setTimeout(() => {
    FIELD_CONFIG.filter(f => f.type === 'tinymce').forEach(f => {
      const id = f.key.replace(/\./g, '-');
      setupTinyMCE(`#${id}`);
    });
  }, 0);
}

// === Parse `.look` → objet
function parseProperties(blob) {
  const lines = blob.split('\n');
  const props = {};
  let currentKey = null;
  let currentValue = [];

  for (let line of lines) {
    if (line.trim().startsWith('#') || line.trim() === '') continue;

    const keyVal = line.match(/^([^=]+)=(.*)$/);
    if (keyVal) {
      if (currentKey) props[currentKey] = currentValue.join('\n');
      currentKey = keyVal[1].trim();
      currentValue = [keyVal[2]];
    } else if (currentKey) {
      currentValue.push(line);
    }
  }
  if (currentKey) props[currentKey] = currentValue.join('\n');
  return props;
}

// === Génère un `.look` à partir d’un objet
function generateBlob(props) {
  return Object.entries(props)
    .map(([key, value]) => `${key}=${value}`)
    .join('\n');
}
