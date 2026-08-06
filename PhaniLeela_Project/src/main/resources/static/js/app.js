// ── STATE ──
let allMenuItems = [];
let filteredItems = [];
let cart = [];
let activeCategory = 'all';
let vegFilter = 'all';
let activeAllergens = [];
let searchQuery = '';
let currentTokenNumber = null;

// ── OWNER PIN STATE ──
const OWNER_PIN = '1234';          // Change this to your secret PIN
let pinBuffer = '';                // digits typed so far
let pinTargetSection = 'orders';  // which section to open after unlock
let pinTargetLink = null;

// ── INIT ──
document.addEventListener('DOMContentLoaded', () => {
  loadStats();
  loadMenu();
  loadPopular();
  initSlider();
  initTokenPreview();
  // Restore owner session if they already unlocked this browser session
  if (sessionStorage.getItem('pl_owner') === 'yes') {
    const nav = document.getElementById('navOrders');
    if (nav) nav.classList.add('owner-unlocked');
  }
});

// ── HERO SLIDER ──
let currentSlideIdx = 0;
let slideTimer = null;

function initSlider() {
  startSlideTimer();
}

function setSlide(index) {
  const slides = document.querySelectorAll('.hero-slide');
  const dots = document.querySelectorAll('.dot');
  if (!slides.length) return;

  currentSlideIdx = (index + slides.length) % slides.length;
  
  slides.forEach((s, i) => s.classList.toggle('active', i === currentSlideIdx));
  dots.forEach((d, i) => d.classList.toggle('active', i === currentSlideIdx));

  resetSlideTimer();
}

function nextSlide() { setSlide(currentSlideIdx + 1); }
function prevSlide() { setSlide(currentSlideIdx - 1); }

function startSlideTimer() {
  slideTimer = setInterval(() => nextSlide(), 4000);
}

function resetSlideTimer() {
  clearInterval(slideTimer);
  startSlideTimer();
}

// ── NAVIGATION ──
function showSection(name, linkEl) {
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
  const sec = document.getElementById(name);
  if (sec) sec.classList.add('active');
  if (linkEl) linkEl.classList.add('active');

  if (name === 'orders') loadOrders();
  if (name === 'graph')  initGraph();
}

// Called when owner clicks Orders nav — checks PIN first
function requireOwnerPin(section, linkEl) {
  // Already unlocked this session?
  if (sessionStorage.getItem('pl_owner') === 'yes') {
    showSection(section, linkEl);
    return;
  }
  // Show PIN modal
  pinBuffer = '';
  pinTargetSection = section;
  pinTargetLink    = linkEl;
  updatePinDots();
  document.getElementById('pinError').classList.add('hidden');
  document.getElementById('pinOverlay').classList.remove('hidden');
  const modal = document.getElementById('pinModal');
  modal.classList.remove('hidden');
  setTimeout(() => modal.classList.add('pin-open'), 10);
}

function closePinModal() {
  const modal = document.getElementById('pinModal');
  modal.classList.remove('pin-open');
  setTimeout(() => {
    modal.classList.add('hidden');
    document.getElementById('pinOverlay').classList.add('hidden');
  }, 250);
  pinBuffer = '';
  updatePinDots();
}

function pinPress(digit) {
  if (pinBuffer.length >= 4) return;
  pinBuffer += digit;
  updatePinDots();
  if (pinBuffer.length === 4) {
    setTimeout(checkPin, 120); // small delay so last dot fills visually
  }
}

function pinClear() {
  if (pinBuffer.length === 0) return;
  pinBuffer = pinBuffer.slice(0, -1);
  updatePinDots();
  document.getElementById('pinError').classList.add('hidden');
}

function updatePinDots() {
  for (let i = 0; i < 4; i++) {
    const dot = document.getElementById('dot' + i);
    if (!dot) continue;
    dot.classList.remove('filled', 'error');
    if (i < pinBuffer.length) dot.classList.add('filled');
  }
}

function checkPin() {
  if (pinBuffer === OWNER_PIN) {
    // Correct PIN — unlock
    sessionStorage.setItem('pl_owner', 'yes');
    const nav = document.getElementById('navOrders');
    if (nav) nav.classList.add('owner-unlocked');
    closePinModal();
    setTimeout(() => showSection(pinTargetSection, pinTargetLink), 280);
  } else {
    // Wrong PIN — shake and reset
    for (let i = 0; i < 4; i++) {
      const dot = document.getElementById('dot' + i);
      if (dot) { dot.classList.remove('filled'); dot.classList.add('error'); }
    }
    document.getElementById('pinError').classList.remove('hidden');
    pinBuffer = '';
    setTimeout(() => {
      for (let i = 0; i < 4; i++) {
        const dot = document.getElementById('dot' + i);
        if (dot) dot.classList.remove('error');
      }
    }, 600);
  }
}

// Owner logout — lock Orders section again
function ownerLogout() {
  sessionStorage.removeItem('pl_owner');
  const nav = document.getElementById('navOrders');
  if (nav) nav.classList.remove('owner-unlocked');
  showSection('home', document.querySelector('.nav-link'));
  showToast('🔒 Owner session ended. Orders section is locked.');
}

function navigateToSection(name) {
  const linkEl = document.querySelector(`.nav-link[href="#${name}"]`);
  showSection(name, linkEl);
  const targetSection = document.getElementById(name);
  if (targetSection) {
    targetSection.scrollIntoView({ behavior: 'smooth' });
  }
}

// ── STATS ──
async function loadStats() {
  try {
    const stats = await Api.getStats();
    animateNumber('stat-menu', stats.totalMenuItems);
    animateNumber('stat-ingredients', stats.totalIngredients);
    animateNumber('stat-orders', stats.totalOrders);
    animateNumber('stat-rels', stats.totalRelationships);
  } catch (e) {
    console.error('Stats error:', e);
  }
}

function animateNumber(id, target) {
  const el = document.getElementById(id);
  if (!el) return;
  let current = 0;
  const step = Math.ceil(target / 40);
  const timer = setInterval(() => {
    current = Math.min(current + step, target);
    el.textContent = current.toLocaleString();
    if (current >= target) clearInterval(timer);
  }, 40);
}

// ── POPULAR ──
async function loadPopular() {
  try {
    const stats = await Api.getStats();
    const grid = document.getElementById('popularGrid');
    if (!grid) return;
    const popular = stats.popularDishes || [];
    if (popular.length === 0) {
      grid.innerHTML = '<div class="empty-state"><p>No orders yet. Be the first!</p></div>';
      return;
    }
    grid.innerHTML = popular.map((d, i) => `
      <div class="popular-card">
        <div class="popular-emoji">${d.emoji}</div>
        <div>
          <div class="popular-rank">#${i+1} Most Ordered</div>
          <div class="popular-name">${d.name}</div>
          <div class="popular-orders">${d.orderCount} orders today</div>
        </div>
        <div class="popular-price">₹${d.price}</div>
      </div>
    `).join('');
  } catch (e) {
    console.error('Popular error:', e);
  }
}

// ── MENU ──
async function loadMenu() {
  try {
    const [items, categories] = await Promise.all([Api.getMenu(), Api.getCategories()]);
    allMenuItems = items;
    filteredItems = [...items];
    renderCategoryTabs(categories);
    renderMenu(filteredItems);
  } catch (e) {
    const grid = document.getElementById('menuGrid');
    if (grid) grid.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>Could not load menu: ${e.message}</p></div>`;
  }
}

function renderCategoryTabs(categories) {
  const tabs = document.getElementById('categoryTabs');
  if (!tabs) return;
  const all = [{ id: 'all', name: 'All Dishes', icon: '🍽️', itemCount: allMenuItems.length }];
  const catList = [...all, ...categories];
  tabs.innerHTML = catList.map(c => `
    <button class="cat-tab ${c.id === activeCategory ? 'active' : ''}" onclick="setCategory('${c.id}', this)">
      ${c.icon} ${c.name} <span style="opacity:0.5;font-size:0.75rem">(${c.itemCount})</span>
    </button>
  `).join('');
}

function setCategory(catId, el) {
  activeCategory = catId;
  document.querySelectorAll('.cat-tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  applyFilters();
}

function setVegFilter(filter) {
  vegFilter = filter;
  ['filterAll','filterVeg','filterNonVeg'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
  });
  const map = { all: 'filterAll', veg: 'filterVeg', nonveg: 'filterNonVeg' };
  const target = document.getElementById(map[filter]);
  if (target) target.classList.add('active');
  applyFilters();
}

function levenshtein(a, b) {
  if (a === b) return 0;
  if (a.length === 0) return b.length;
  if (b.length === 0) return a.length;
  const matrix = [];
  for (let i = 0; i <= b.length; i++) matrix[i] = [i];
  for (let j = 0; j <= a.length; j++) matrix[0][j] = j;

  for (let i = 1; i <= b.length; i++) {
    for (let j = 1; j <= a.length; j++) {
      if (b.charAt(i - 1) === a.charAt(j - 1)) {
        matrix[i][j] = matrix[i - 1][j - 1];
      } else {
        matrix[i][j] = Math.min(
          matrix[i - 1][j - 1] + 1,
          matrix[i][j - 1] + 1,
          matrix[i - 1][j] + 1
        );
      }
    }
  }
  return matrix[b.length][a.length];
}

function normalizeSearchTerm(str) {
  if (!str) return '';
  let s = str.toLowerCase().trim();
  s = s.replace(/briyani|biriyani|birani|bryani/g, 'biryani');
  s = s.replace(/panner|paner|paneir/g, 'paneer');
  s = s.replace(/chiken|chikn|chickn|chikenn/g, 'chicken');
  s = s.replace(/samose|samoosa|smosa/g, 'samosa');
  s = s.replace(/naan|nan/g, 'naan');
  s = s.replace(/lasi|lassee/g, 'lassi');
  s = s.replace(/muton|mutten/g, 'mutton');
  s = s.replace(/kabab|kebab/g, 'kebab');
  s = s.replace(/paratha|parata|paratta/g, 'paratha');
  s = s.replace(/tikka|tika/g, 'tikka');
  s = s.replace(/gulab|gullab/g, 'gulab');
  s = s.replace(/kulfi|khulfi/g, 'kulfi');
  return s;
}

function tokenMatchesWord(token, targetWords, rawTargetText) {
  if (!token) return true;
  if (rawTargetText.includes(token)) return true;

  const maxDist = token.length <= 4 ? 1 : 2;

  for (let word of targetWords) {
    const cleanWord = word.replace(/[^a-z0-9]/g, '');
    if (!cleanWord) continue;

    if (token.length >= 3 && cleanWord.startsWith(token)) return true;

    if (token.length >= 3 && Math.abs(cleanWord.length - token.length) <= maxDist) {
      if (levenshtein(token, cleanWord) <= maxDist) return true;
    }
  }
  return false;
}

function itemMatchesSearch(item, q) {
  if (!q) return true;
  const rawQuery = q.trim().toLowerCase();
  if (!rawQuery) return true;

  const normalizedQuery = normalizeSearchTerm(rawQuery);

  let ingredientsText = '';
  if (item.ingredients && Array.isArray(item.ingredients)) {
    ingredientsText = item.ingredients
      .map(ing => (typeof ing === 'object' ? (ing.name || '') : String(ing)))
      .join(' ');
  }

  const targetText = (
    (item.name || '') + ' ' +
    (item.description || '') + ' ' +
    (item.categoryName || '') + ' ' +
    (item.allergens ? item.allergens.join(' ') : '') + ' ' +
    ingredientsText
  ).toLowerCase();

  const normTarget = normalizeSearchTerm(targetText);

  const tokens = normalizedQuery.split(/\s+/).filter(Boolean);
  const targetWords = normTarget.split(/\s+/).filter(Boolean);

  return tokens.every(token => tokenMatchesWord(token, targetWords, normTarget));
}

function applyAllergenFilter() {
  const checked = Array.from(document.querySelectorAll('.allergen-chip input:checked'))
    .map(cb => cb.value);
  document.querySelectorAll('.allergen-chip').forEach(chip => {
    const input = chip.querySelector('input');
    chip.classList.toggle('checked', input ? input.checked : false);
  });
  activeAllergens = checked;
  applyFilters();
}

function applyFilters() {
  let result = [...allMenuItems];
  const q = searchQuery.trim();

  // Allergen exclusion filter
  if (activeAllergens.length > 0) {
    result = result.filter(item => {
      if (!item.allergens || item.allergens.length === 0) return true;
      return !item.allergens.some(a => 
        a && activeAllergens.some(excluded => excluded.toLowerCase() === a.toLowerCase())
      );
    });
  }

  // Veg / Non-Veg filter
  if (vegFilter === 'veg') result = result.filter(i => i.isVeg);
  if (vegFilter === 'nonveg') result = result.filter(i => !i.isVeg);

  // Search or Category filter
  if (q) {
    result = result.filter(i => itemMatchesSearch(i, q));
  } else if (activeCategory !== 'all') {
    result = result.filter(i => i.categoryId === activeCategory);
  }

  filteredItems = result;
  renderMenu(result);
}

function searchDishes() {
  searchQuery = document.getElementById('searchInput').value;
  applyFilters();
}

const DISH_IMAGES = {
  'dish-001': 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=600&q=80',
  'dish-002': 'https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?auto=format&fit=crop&w=600&q=80',
  'dish-003': 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?auto=format&fit=crop&w=600&q=80',
  'dish-004': '/images/aloo-tikki.png',
  'dish-005': 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=600&q=80',
  'dish-006': 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=600&q=80',
  'dish-007': 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?auto=format&fit=crop&w=600&q=80',
  'dish-008': 'https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?auto=format&fit=crop&w=600&q=80',
  'dish-009': 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=600&q=80',
  'dish-010': 'https://images.unsplash.com/photo-1645177628172-a94c1f96e6db?auto=format&fit=crop&w=600&q=80',
  'dish-011': 'https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=600&q=80',
  'dish-012': 'https://images.unsplash.com/photo-1545247181-516773cae754?auto=format&fit=crop&w=600&q=80',
  'dish-013': 'https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?auto=format&fit=crop&w=600&q=80',
  'dish-014': 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=600&q=80',
  'dish-015': '/images/chicken-biryani.png',
  'dish-016': 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=600&q=80',
  'dish-017': '/images/paneer-biryani.png',
  'dish-018': 'https://images.unsplash.com/photo-1596797038530-2c107229654b?auto=format&fit=crop&w=600&q=80',
  'dish-019': '/images/butter-naan.png',
  'dish-020': '/images/garlic-naan.png',
  'dish-021': '/images/tandoori-roti.png',
  'dish-022': '/images/lachha-paratha.png',
  'dish-023': '/images/gulab-jamun.png',
  'dish-024': '/images/rasmalai.png',
  'dish-025': 'https://images.unsplash.com/photo-1517244683847-7456b63c5969?auto=format&fit=crop&w=600&q=80',
  'dish-026': '/images/jalebi.png',
  'dish-027': '/images/sweet-lassi.png',
  'dish-028': 'https://images.unsplash.com/photo-1546173159-315724a31696?auto=format&fit=crop&w=600&q=80',
  'dish-029': 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=600&q=80',
  'dish-030': 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=80',
  'dish-031': '/images/mango-kulfi.png',
  'dish-032': 'https://images.unsplash.com/photo-1570197788417-0e82375c9371?auto=format&fit=crop&w=600&q=80',
  'dish-033': 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=600&q=80',
  'dish-034': 'https://images.unsplash.com/photo-1570197788417-0e82375c9371?auto=format&fit=crop&w=600&q=80',
  'dish-035': 'https://images.unsplash.com/photo-1580915411954-282cb1b0d780?auto=format&fit=crop&w=600&q=80'
};

function getDishImg(id) {
  return DISH_IMAGES[id] || '/images/hero-thali.png';
}

function renderMenu(items) {
  const grid = document.getElementById('menuGrid');
  if (!grid) return;
  if (items.length === 0) {
    grid.innerHTML = '<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">🍽️</div><p>No dishes found for your filters.</p></div>';
    return;
  }
  grid.innerHTML = items.map(item => `
    <div class="dish-card" onclick="openDishModal('${item.id}')">
      <div class="dish-photo-area">
        <img src="${getDishImg(item.id)}" alt="${item.name}" class="dish-card-img" />
        <div class="veg-indicator ${item.isVeg ? 'veg' : 'nonveg'}"></div>
      </div>
      <div class="dish-info">
        <div class="dish-name">${item.name}</div>
        <div class="dish-desc">${item.description || ''}</div>
        ${item.allergens && item.allergens.filter(a=>a).length > 0 ? `
          <div class="dish-allergens">${item.allergens.filter(a=>a).map(a => `<span class="allergen-badge">⚠️ ${a}</span>`).join('')}</div>
        ` : ''}
        <div class="dish-footer">
          <span class="dish-price">₹${item.price}</span>
          <button class="add-btn" onclick="event.stopPropagation(); addToCart('${item.id}', '${escHtml(item.name)}', ${item.price}, '${item.imageEmoji || '🍽️'}')">+ Add</button>
        </div>
      </div>
    </div>
  `).join('');
}

function escHtml(str) { return str.replace(/'/g, "\\'"); }

// ── DISH MODAL ──
async function openDishModal(id) {
  const overlay = document.getElementById('modalOverlay');
  const modal = document.getElementById('dishModal');
  const content = document.getElementById('modalContent');
  overlay.classList.remove('hidden');
  modal.classList.remove('hidden');
  setTimeout(() => modal.classList.add('open'), 10);
  content.innerHTML = '<div class="loading-spinner"></div>';

  try {
    const item = await Api.getMenuItem(id);
    const allergenHtml = item.allergens && item.allergens.filter(a=>a).length > 0
      ? item.allergens.filter(a=>a).map(a => `<span class="allergen-badge">⚠️ ${a}</span>`).join('')
      : '<span style="color:var(--muted);font-size:0.8rem">No common allergens</span>';
    const ingHtml = (item.ingredients || []).filter(i=>i.name).map(i =>
      `<span class="ing-chip" title="${i.amount||''}">🌿 ${i.name}</span>`
    ).join('');

    // Build pairsWith list — use API result first, then smart client-side fallback
    let pairsData = (item.pairsWith || []).filter(p => p && p.id && p.name);

    // If API returned no pairs, generate smart fallback from already-loaded menu
    if (pairsData.length === 0 && allMenuItems.length > 0) {
      const PREFERRED_COMPANION_CATS = {
        'cat-starters': ['cat-main-veg', 'cat-main-nonveg', 'cat-beverages'],
        'cat-main-veg': ['cat-breads', 'cat-beverages', 'cat-desserts'],
        'cat-main-nonveg': ['cat-breads', 'cat-beverages', 'cat-desserts'],
        'cat-biryani': ['cat-starters', 'cat-beverages', 'cat-desserts'],
        'cat-breads': ['cat-main-veg', 'cat-main-nonveg', 'cat-beverages'],
        'cat-desserts': ['cat-beverages', 'cat-icecream'],
        'cat-beverages': ['cat-desserts', 'cat-main-veg', 'cat-starters'],
        'cat-icecream': ['cat-beverages', 'cat-desserts']
      };

      // Find current item's category
      const currentItem = allMenuItems.find(m => m.id === id);
      const currentCatId = currentItem ? currentItem.categoryId : '';
      const preferredCats = PREFERRED_COMPANION_CATS[currentCatId] || ['cat-breads', 'cat-beverages', 'cat-desserts'];

      // Pick items from preferred companion categories (excluding current dish)
      let candidates = allMenuItems.filter(m => m.id !== id && preferredCats.includes(m.categoryId));

      // Shuffle for variety
      candidates = candidates.sort(() => Math.random() - 0.5).slice(0, 4);

      // If still not enough, top up with random dishes
      if (candidates.length < 3) {
        const others = allMenuItems.filter(m => m.id !== id && !candidates.find(c => c.id === m.id));
        candidates = [...candidates, ...others.sort(() => Math.random() - 0.5)].slice(0, 4);
      }

      pairsData = candidates.map(m => ({
        id: m.id, name: m.name, price: m.price,
        imageEmoji: m.imageEmoji || '🍽️', isVeg: m.isVeg, categoryName: m.categoryName
      }));
    }

    const pairsHtml = pairsData.map(p => `
      <div class="pair-card" style="display:flex;align-items:center;gap:12px;padding:10px 12px;background:rgba(255,255,255,0.04);border:1px solid var(--border);border-radius:12px;margin-bottom:8px;cursor:pointer;transition:background 0.2s;" 
           onmouseover="this.style.background='rgba(255,255,255,0.08)'" 
           onmouseout="this.style.background='rgba(255,255,255,0.04)'"
           onclick="closeDishModal(); setTimeout(()=>openDishModal('${p.id}'),300)">
        <img src="${getDishImg(p.id)}" alt="${p.name}" style="width:52px;height:52px;border-radius:10px;object-fit:cover;flex-shrink:0;" 
             onerror="this.style.display='none'"/>
        <div style="flex:1;min-width:0;">
          <div style="font-weight:600;font-size:0.9rem;color:var(--fg);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${p.name}</div>
          <div style="color:var(--muted);font-size:0.75rem;">${p.categoryName || ''}</div>
          <div style="color:var(--primary);font-size:0.85rem;font-weight:700;">₹${p.price}</div>
        </div>
        <div class="pair-action-wrap" onclick="event.stopPropagation()">
          ${renderPairControl(p)}
        </div>
      </div>
    `).join('');

    content.innerHTML = `
      <div class="modal-photo-banner">
        <img src="${getDishImg(item.id)}" alt="${item.name}" class="modal-banner-img" />
      </div>
      <div class="modal-title">${item.name}</div>
      <div class="modal-cat">${item.categoryName || 'Menu Item'} • <span class="${item.isVeg ? 'veg-text' : 'nonveg-text'}">${item.isVeg ? '🟢 Veg' : '🔴 Non-Veg'}</span></div>
      <div class="modal-desc">${item.description || ''}</div>
      <div class="modal-price">₹${item.price}</div>
      
      <div class="modal-section-title">Ingredients</div>
      <div class="ingredients-grid">${ingHtml || '<span style="color:var(--muted);font-size:0.8rem">No ingredients listed</span>'}</div>
      
      <div class="modal-section-title">Allergens</div>
      <div class="allergen-list">${allergenHtml}</div>
      
      <div class="modal-section-title">🤝 Pairs Well With</div>
      <div class="pairs-grid">${pairsHtml || '<span style="color:var(--muted);font-size:0.8rem">No suggestions available</span>'}</div>
      
      <button class="modal-add-btn" onclick="addToCart('${item.id}', '${escHtml(item.name)}', ${item.price}, '${(item.imageEmoji||'🍽️').replace(/'/g,"\\'")}'); closeDishModal();">Add to Cart 🛒</button>
    `;
  } catch (e) {
    content.innerHTML = `<div class="empty-state"><p>Could not load dish details.</p></div>`;
  }
}

function closeDishModal() {
  const overlay = document.getElementById('modalOverlay');
  const modal = document.getElementById('dishModal');
  modal.classList.remove('open');
  setTimeout(() => {
    overlay.classList.add('hidden');
    modal.classList.add('hidden');
  }, 250);
}

// ── CART ──
function toggleCart() {
  const sidebar = document.getElementById('cartSidebar');
  const overlay = document.getElementById('cartOverlay');
  const isOpen = sidebar.classList.contains('open');
  
  if (isOpen) {
    sidebar.classList.remove('open');
    setTimeout(() => {
      sidebar.classList.add('hidden');
      overlay.classList.add('hidden');
    }, 300);
  } else {
    sidebar.classList.remove('hidden');
    overlay.classList.remove('hidden');
    setTimeout(() => sidebar.classList.add('open'), 10);
  }
}

function addToCart(id, name, price, emoji) {
  const existing = cart.find(i => i.id === id);
  if (existing) {
    existing.quantity++;
  } else {
    cart.push({ id, name, price, emoji, quantity: 1 });
  }
  updateCartUI();
  showToast('Added ' + name + ' to cart!');
}

function updateCartItem(id, change) {
  const item = cart.find(i => i.id === id);
  if (!item) return;
  item.quantity += change;
  if (item.quantity <= 0) {
    cart = cart.filter(i => i.id !== id);
  }
  updateCartUI();
  updatePairCtrlDOM(id, item.name, item.price, item.emoji);
}

function renderPairControl(p) {
  const inCart = cart.find(c => c.id === p.id);
  const safeName = escHtml(p.name);
  const safeEmoji = (p.imageEmoji || '🍽️').replace(/'/g, "\\'");
  
  if (inCart) {
    return `
      <div class="pair-qty-control" id="pair-ctrl-${p.id}" onclick="event.stopPropagation()">
        <button class="pair-qty-btn" onclick="event.stopPropagation(); changePairQty('${p.id}', -1, '${safeName}', ${p.price}, '${safeEmoji}')">-</button>
        <span class="pair-qty-num">${inCart.quantity}</span>
        <button class="pair-qty-btn" onclick="event.stopPropagation(); changePairQty('${p.id}', 1, '${safeName}', ${p.price}, '${safeEmoji}')">+</button>
      </div>
    `;
  } else {
    return `
      <button class="add-btn" id="pair-ctrl-${p.id}" style="padding:5px 14px;font-size:0.82rem;flex-shrink:0;" 
              onclick="event.stopPropagation(); changePairQty('${p.id}', 1, '${safeName}', ${p.price}, '${safeEmoji}')">+ Add</button>
    `;
  }
}

function changePairQty(id, change, name, price, emoji) {
  const existing = cart.find(i => i.id === id);
  if (!existing && change > 0) {
    cart.push({ id, name, price, emoji, quantity: 1 });
    showToast('Added ' + name + ' to cart!');
  } else if (existing) {
    existing.quantity += change;
    if (existing.quantity <= 0) {
      cart = cart.filter(i => i.id !== id);
    }
  }
  updateCartUI();
  updatePairCtrlDOM(id, name, price, emoji);
}

function updatePairCtrlDOM(id, name, price, emoji) {
  const ctrl = document.getElementById(`pair-ctrl-${id}`);
  if (!ctrl) return;
  const inCart = cart.find(c => c.id === id);
  const safeName = escHtml(name);
  const safeEmoji = (emoji || '🍽️').replace(/'/g, "\\'");
  const container = ctrl.parentElement;
  if (!container) return;

  if (inCart) {
    container.innerHTML = `
      <div class="pair-qty-control" id="pair-ctrl-${id}" onclick="event.stopPropagation()">
        <button class="pair-qty-btn" onclick="event.stopPropagation(); changePairQty('${id}', -1, '${safeName}', ${price}, '${safeEmoji}')">-</button>
        <span class="pair-qty-num">${inCart.quantity}</span>
        <button class="pair-qty-btn" onclick="event.stopPropagation(); changePairQty('${id}', 1, '${safeName}', ${price}, '${safeEmoji}')">+</button>
      </div>
    `;
  } else {
    container.innerHTML = `
      <button class="add-btn" id="pair-ctrl-${id}" style="padding:5px 14px;font-size:0.82rem;flex-shrink:0;" 
              onclick="event.stopPropagation(); changePairQty('${id}', 1, '${safeName}', ${price}, '${safeEmoji}')">+ Add</button>
    `;
  }
}

function setMobileNavActive(btn) {
  if (!btn) return;
  document.querySelectorAll('.mobile-nav-item').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
}

function updateCartUI() {
  const itemsContainer = document.getElementById('cartItems');
  const footer = document.getElementById('cartFooter');
  const countEl = document.getElementById('cartCount');
  const countMobileEl = document.getElementById('cartCountMobile');
  const totalEl = document.getElementById('cartTotal');

  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  if (countEl) countEl.textContent = totalItems;
  if (countMobileEl) countMobileEl.textContent = totalItems;

  if (cart.length === 0) {
    itemsContainer.innerHTML = '<p class="cart-empty">Your cart is empty.<br>Add some dishes!</p>';
    footer.style.display = 'none';
    return;
  }

  footer.style.display = 'block';

  itemsContainer.innerHTML = cart.map(item => `
    <div class="cart-item">
      <div>
        <div class="cart-item-name">${item.emoji} ${item.name}</div>
        <div class="cart-item-price">₹${item.price}</div>
      </div>
      <div class="cart-item-qty">
        <button class="qty-btn" onclick="updateCartItem('${item.id}', -1)">-</button>
        <span>${item.quantity}</span>
        <button class="qty-btn" onclick="updateCartItem('${item.id}', 1)">+</button>
      </div>
    </div>
  `).join('');

  const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  totalEl.textContent = '₹' + total;
}

// ── TOKEN SYSTEM ──

// Generate a daily-resetting sequential token number
function generateNextToken() {
  const today = new Date().toISOString().slice(0, 10); // "2026-08-05"
  const stored = JSON.parse(localStorage.getItem('pl_token') || '{"date":"","count":0}');
  const count = (stored.date === today) ? stored.count + 1 : 1;
  localStorage.setItem('pl_token', JSON.stringify({ date: today, count }));
  return count;
}

function getTokenDisplay(num) {
  return String(num).padStart(3, '0');  // 001, 002 ... 099, 100
}

function initTokenPreview() {
  // When user types their name, preview a token number
  const nameInput = document.getElementById('customerName');
  if (!nameInput) return;
  nameInput.addEventListener('input', function() {
    const previewEl = document.getElementById('tokenPreviewNumber');
    if (!previewEl) return;
    if (this.value.trim()) {
      // Peek at what token will be assigned (without incrementing)
      const today = new Date().toISOString().slice(0, 10);
      const stored = JSON.parse(localStorage.getItem('pl_token') || '{"date":"","count":0}');
      const nextCount = (stored.date === today) ? stored.count + 1 : 1;
      previewEl.textContent = '#' + getTokenDisplay(nextCount);
    } else {
      previewEl.textContent = '—';
    }
  });
}

// ── PAYMENT METHOD SELECTION ──
let selectedPaymentMethod = 'UPI';

function selectPaymentMethod(method, btnEl) {
  selectedPaymentMethod = method;
  document.querySelectorAll('.pay-method-btn').forEach(b => b.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');

  // Show corresponding details box
  const boxes = { UPI: 'payDetailsUpi', CARD: 'payDetailsCard', CASH: 'payDetailsCash' };
  Object.keys(boxes).forEach(m => {
    const box = document.getElementById(boxes[m]);
    if (box) {
      if (m === method) box.classList.remove('hidden');
      else box.classList.add('hidden');
    }
  });
}

function showTokenModal(tokenNum, customerName, payMethod = 'UPI', payStatus = 'PAID', total = 0) {
  const overlay = document.getElementById('tokenOverlay');
  const modal   = document.getElementById('tokenModal');
  const numEl   = document.getElementById('tokenModalNumber');
  const nameEl  = document.getElementById('tokenModalName');
  const badgeEl = document.getElementById('tokenModalPaymentBadge');
  const txtEl   = document.getElementById('tokenPaymentStatusTxt');
  if (!modal) return;

  numEl.textContent  = '#' + getTokenDisplay(tokenNum);
  nameEl.textContent = '👤 ' + customerName;

  const isPaid = payStatus === 'PAID';
  const methodLabel = payMethod === 'UPI' ? '📱 UPI (GPay/PhonePe)' : (payMethod === 'CARD' ? '💳 Card' : '💵 Cash at Counter');

  if (badgeEl) {
    badgeEl.innerHTML = isPaid
      ? `<span class="pay-badge-paid">✓ PAID ONLINE VIA ${payMethod} (₹${total})</span>`
      : `<span class="pay-badge-pending">⏳ PAY ₹${total} AT COUNTER</span>`;
  }

  if (txtEl) {
    txtEl.textContent = isPaid ? `PAID VIA ${payMethod}` : 'PAY AT COUNTER ⏳';
    txtEl.style.color = isPaid ? 'var(--success)' : '#f59e0b';
  }

  overlay.classList.remove('hidden');
  modal.classList.remove('hidden');
  setTimeout(() => modal.classList.add('token-modal-open'), 10);
}

function closeTokenModal() {
  const overlay = document.getElementById('tokenOverlay');
  const modal   = document.getElementById('tokenModal');
  if (!modal) return;
  modal.classList.remove('token-modal-open');
  setTimeout(() => {
    overlay.classList.add('hidden');
    modal.classList.add('hidden');
  }, 250);
}

async function placeOrder() {
  const name = document.getElementById('customerName').value.trim();
  const note = document.getElementById('specialNote').value.trim();

  if (!name) {
    showToast('Please enter your name to get a token', true);
    return;
  }
  if (cart.length === 0) {
    showToast('Your cart is empty — add some dishes first!', true);
    return;
  }

  // Calculate order total
  const totalAmount = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

  // Generate the token number
  const tokenNum  = generateNextToken();
  const tokenCode = 'TOKEN-' + getTokenDisplay(tokenNum);

  // Payment status
  const payStatus = (selectedPaymentMethod === 'CASH') ? 'PAY AT COUNTER' : 'PAID';

  const items = cart.map(i => ({
    id: i.id,
    menuItemId: i.id,
    quantity: i.quantity,
    specialInstructions: note
  }));

  try {
    await Api.placeOrder({
      customerName: name,
      tableNumber: tokenCode,  // reuse tableNumber field to store token
      tokenNumber: tokenNum,
      paymentMethod: selectedPaymentMethod,
      paymentStatus: payStatus,
      specialNote: note,
      items
    });

    // Clear cart
    cart = [];
    updateCartUI();
    toggleCart();
    document.getElementById('customerName').value = '';
    document.getElementById('specialNote').value  = '';
    document.getElementById('tokenPreviewNumber').textContent = '—';

    // Show big token confirmation popup
    showTokenModal(tokenNum, name, selectedPaymentMethod, payStatus, totalAmount);

    loadStats();
    loadPopular();
    loadOrders();
  } catch (e) {
    showToast('Failed to place order: ' + e.message, true);
    // Roll back the token counter
    const today = new Date().toISOString().slice(0, 10);
    const stored = JSON.parse(localStorage.getItem('pl_token') || '{"date":"","count":0}');
    if (stored.date === today && stored.count > 0) {
      localStorage.setItem('pl_token', JSON.stringify({ date: today, count: stored.count - 1 }));
    }
  }
}

// ── ORDERS LIST ──
async function loadOrders() {
  const list = document.getElementById('ordersList');
  if (!list) return;
  list.innerHTML = '<div class="loading-spinner"></div>';

  try {
    const orders = await Api.getOrders();
    if (orders.length === 0) {
      list.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>No recent orders found.</p></div>';
      return;
    }

    list.innerHTML = orders.map(o => {
      const isServed    = o.status.toLowerCase() === 'served';
      const statusClass = isServed ? 'status-served' : 'status-preparing';
      const itemsHtml   = (o.items || []).map(i =>
        `<span class="order-item-chip">${i.quantity || 1}x ${i.name || i.menuItemName || 'Dish'}</span>`
      ).join('');

      // Parse token from tableNumber field (e.g. "TOKEN-001" or legacy "T1")
      const rawTable    = o.tableNumber || '';
      const isToken     = rawTable.startsWith('TOKEN-');
      const tokenBadge  = isToken
        ? `<span class="token-badge">🎫 Token ${rawTable.replace('TOKEN-', '#')}</span>`
        : `<span class="token-badge" style="background:rgba(99,102,241,0.15);border-color:rgba(99,102,241,0.3);color:#818cf8">📋 Table ${rawTable}</span>`;

      return `
        <div class="order-card" style="${isServed ? 'border-color:rgba(34,197,94,0.3);' : ''}">
          <div class="order-top">
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
              ${tokenBadge}
              <div>
                <div class="order-customer">${o.customerName || 'Guest'}</div>
                <div class="order-table"><span class="order-id">#${(o.id||'').substring(0,14)}</span></div>
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:10px">
              <div class="order-status ${statusClass}">${o.status}</div>
              ${!isServed ? `<button class="btn-sm" onclick="toggleOrderStatus('${o.id}', 'Served')">Mark Served 🟢</button>` : ''}
              <button class="btn-sm" style="background:rgba(239,68,68,0.15);color:#ef4444;border-color:rgba(239,68,68,0.3)" onclick="removeOrder('${o.id}')">&#128465;&#65039; Remove</button>
            </div>
          </div>
          <div class="order-items">${itemsHtml}</div>
          <div class="order-total">Total: ₹${o.totalAmount || 0}</div>
        </div>
      `;
    }).join('');
  } catch (e) {
    list.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>Could not load orders: ${e.message}</p></div>`;
  }
}

async function toggleOrderStatus(id, newStatus) {
  try {
    await Api.updateOrderStatus(id, newStatus);
    showToast(`Order status updated to ${newStatus}!`);
    loadOrders();
  } catch (e) {
    showToast('Could not update status: ' + e.message, true);
  }
}

async function removeOrder(id) {
  if (!confirm('Are you sure you want to remove this order?')) return;
  try {
    await Api.deleteOrder(id);
    showToast('Order removed successfully! 🗑️');
    loadOrders();
    loadStats();
  } catch (e) {
    showToast('Failed to remove order: ' + e.message, true);
  }
}

async function resetAllOrders() {
  if (!confirm('Are you sure you want to remove ALL orders? This cannot be undone.')) return;
  try {
    await Api.deleteAllOrders();
    showToast('All orders cleared! 🗑️');
    loadOrders();
    loadStats();
  } catch (e) {
    showToast('Failed to reset orders: ' + e.message, true);
  }
}

// ── TOAST ──
let toastTimeout;
function showToast(message, isError = false) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  
  toast.textContent = message;
  toast.style.borderColor = isError ? 'var(--nonveg)' : 'var(--veg)';
  toast.style.color = isError ? 'var(--nonveg)' : 'var(--text)';
  
  toast.classList.remove('hidden');
  setTimeout(() => toast.classList.add('show'), 10);
  
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.classList.add('hidden'), 300);
  }, 3000);
}
