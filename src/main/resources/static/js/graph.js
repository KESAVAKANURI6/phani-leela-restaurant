// Graph visualization using Cytoscape.js
let cyInstance = null;

async function initGraph() {
  const canvas = document.getElementById('graphCanvas');
  if (!canvas) return;

  if (cyInstance) {
    setTimeout(() => {
      cyInstance.resize();
      cyInstance.fit(null, 40);
    }, 100);
    return;
  }

  canvas.innerHTML = '<div class="loading-spinner"></div>';

  try {
    const data = await Api.getGraph();
    canvas.innerHTML = '';

    if (typeof cytoscape === 'undefined') {
      renderFallbackGraphList(canvas, data);
      return;
    }

    cyInstance = cytoscape({
      container: canvas,
      elements: [...data.nodes, ...data.edges],
      style: [
        {
          selector: 'node',
          style: {
            'label': 'data(label)',
            'text-valign': 'center',
            'text-halign': 'center',
            'font-family': 'Lato, sans-serif',
            'font-size': '10px',
            'font-weight': 'bold',
            'color': '#ffffff',
            'text-outline-width': 2,
            'text-outline-color': '#000000',
            'text-wrap': 'wrap',
            'text-max-width': '90px',
            'width': 55, 'height': 55,
            'border-width': 2,
            'border-color': 'rgba(255,255,255,0.2)'
          }
        },
        {
          selector: 'node[type="menuitem"]',
          style: {
            'background-color': '#e85d04',
            'shape': 'ellipse',
            'width': 65, 'height': 65,
            'border-color': '#fbbf24',
            'border-width': 3
          }
        },
        {
          selector: 'node[type="ingredient"]',
          style: {
            'background-color': '#16a34a',
            'shape': 'round-rectangle',
            'width': 50, 'height': 35
          }
        },
        {
          selector: 'node[type="category"]',
          style: {
            'background-color': '#4f46e5',
            'shape': 'diamond',
            'width': 60, 'height': 60
          }
        },
        {
          selector: 'node[type="allergen"]',
          style: {
            'background-color': '#dc2626',
            'shape': 'star',
            'width': 55, 'height': 55
          }
        },
        {
          selector: 'node:selected',
          style: { 'border-color': '#fbbf24', 'border-width': 5 }
        },
        {
          selector: 'node.highlighted',
          style: {
            'border-color': '#fbbf24',
            'border-width': 6,
            'opacity': 1,
            'z-index': 999
          }
        },
        {
          selector: 'node.faded',
          style: { 'opacity': 0.08, 'z-index': 1 }
        },
        {
          selector: 'edge',
          style: {
            'width': 2,
            'line-color': 'rgba(232,93,4,0.4)',
            'target-arrow-color': 'rgba(232,93,4,0.6)',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            'font-size': '8px',
            'color': '#9e8a76'
          }
        },
        {
          selector: 'edge.highlighted',
          style: {
            'line-color': '#fbbf24',
            'target-arrow-color': '#fbbf24',
            'width': 4,
            'opacity': 1,
            'z-index': 999
          }
        },
        {
          selector: 'edge.faded',
          style: { 'opacity': 0.03, 'z-index': 1 }
        },
        {
          selector: 'edge:selected',
          style: {
            'line-color': '#fbbf24',
            'target-arrow-color': '#fbbf24',
            'width': 4
          }
        }
      ],
      layout: {
        name: 'concentric',
        concentric: function(node) {
          const type = node.data('type');
          if (type === 'category')   return 4;
          if (type === 'menuitem')   return 3;
          if (type === 'ingredient') return 2;
          return 1;
        },
        levelWidth: function() { return 1; },
        padding: 50,
        animate: false,
        fit: true
      },
      wheelSensitivity: 0.3
    });

    setTimeout(() => {
      if (cyInstance) { cyInstance.resize(); cyInstance.fit(null, 40); }
    }, 200);

    // Populate dish dropdown
    populateDishDropdown(data);

    // ── Tooltip ──
    const tooltip = document.getElementById('graphTooltip');

    // User-friendly type labels
    const typeLabels = {
      menuitem:   { label: '🍽️ Dish',           color: '#e85d04' },
      ingredient: { label: '🌿 Ingredient',      color: '#22c55e' },
      category:   { label: '📂 Food Category',  color: '#6366f1' },
      allergen:   { label: '⚠️ Allergen',        color: '#ef4444' }
    };

    cyInstance.on('tap', 'node', function(evt) {
      const node = evt.target;
      const d    = node.data();
      const pos  = evt.renderedPosition;
      const rect = canvas.getBoundingClientRect();
      const typeInfo = typeLabels[d.type] || { label: d.type, color: '#fff' };

      if (d.type === 'menuitem') {
        highlightDishRoute(node);
        const sel = document.getElementById('dishGraphSelect');
        if (sel) sel.value = d.id;
        inspectDish(d.id);
      } else {
        // For non-dish nodes (ingredient, category, allergen), highlight immediate neighbors
        const directEdges = node.connectedEdges();
        const neighbors = node.neighborhood();
        const route = node.union(directEdges).union(neighbors);
        cyInstance.elements().removeClass('highlighted').addClass('faded');
        route.removeClass('faded').addClass('highlighted');
      }

      let details = '';
      if (d.type === 'menuitem') {
        details = `
          <div class="tooltip-detail">${d.isVeg ? '🟢 Vegetarian' : '🔴 Non-Vegetarian'}</div>
          <div class="tooltip-detail" style="color:#fbbf24;font-weight:700;font-size:1rem;margin-top:4px">${d.emoji || ''} ₹${d.price || ''}</div>
          <div style="font-size:0.75rem;color:#9e8a76;margin-top:6px">Showing dish ingredients & category</div>
        `;
      } else if (d.type === 'ingredient') {
        details = `<div class="tooltip-detail">Found in multiple dishes</div>
          <div style="font-size:0.75rem;color:#9e8a76;margin-top:4px">Type: ${d.ingredientType || 'Ingredient'}</div>`;
      } else if (d.type === 'category') {
        details = `<div class="tooltip-detail">Groups dishes of the same type</div>`;
      } else if (d.type === 'allergen') {
        details = `<div class="tooltip-detail" style="color:#ef4444">⚠️ May cause allergic reactions</div>
          <div style="font-size:0.75rem;color:#9e8a76;margin-top:4px">Use allergen filter in Menu to exclude</div>`;
      }

      tooltip.innerHTML = `
        <div class="tooltip-title">${d.label || ''}</div>
        <div class="tooltip-type" style="color:${typeInfo.color}">${typeInfo.label}</div>
        ${details}
      `;
      tooltip.style.left = Math.min(rect.left + pos.x + 20, window.innerWidth - 230) + 'px';
      tooltip.style.top  = Math.min(rect.top  + pos.y - 30, window.innerHeight - 170) + 'px';
      tooltip.classList.remove('hidden');
    });

    cyInstance.on('tap', function(evt) {
      if (evt.target === cyInstance) {
        tooltip.classList.add('hidden');
        cyInstance.elements().removeClass('highlighted faded');
      }
    });

    cyInstance.on('zoom pan', function() { tooltip.classList.add('hidden'); });

  } catch (err) {
    canvas.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><p>${err.message}</p></div>`;
  }
}

// ── Highlight exact dish route (dish + its ingredients + category + allergen) ──
function highlightDishRoute(node) {
  if (!cyInstance || !node || node.length === 0) return;

  // 1. Edges connected directly to THIS dish
  const dishEdges = node.connectedEdges();

  // 2. Direct neighbor nodes (Ingredients & Category of THIS dish)
  const directTargets = dishEdges.targets().union(dishEdges.sources());

  // 3. Allergen edges originating ONLY from these ingredient nodes
  const ingredientNodes = directTargets.filter(n => n.data('type') === 'ingredient');
  const allergenEdges = ingredientNodes.connectedEdges().filter(e => {
    const target = e.target();
    return target && target.data && target.data('type') === 'allergen';
  });
  const allergenNodes = allergenEdges.targets();

  // Combine into exact dish route
  const exactRoute = node
    .union(dishEdges)
    .union(directTargets)
    .union(allergenEdges)
    .union(allergenNodes);

  // Fade everything else, highlight exactRoute ONLY
  cyInstance.elements().removeClass('highlighted').addClass('faded');
  exactRoute.removeClass('faded').addClass('highlighted');

  return exactRoute;
}

function populateDishDropdown(data) {
  const sel = document.getElementById('dishGraphSelect');
  if (!sel) return;
  const menuNodes = (data.nodes || [])
    .filter(n => n.data.type === 'menuitem')
    .map(n => ({ id: n.data.id, name: n.data.label }))
    .sort((a, b) => a.name.localeCompare(b.name));

  sel.innerHTML = '<option value="">-- Choose a Dish (e.g. Butter Chicken) --</option>' +
    menuNodes.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
}

function onDishDropdownSelect(dishId) {
  if (!dishId) {
    clearDishInspection();
    return;
  }
  inspectDish(dishId);

  if (cyInstance) {
    const node = cyInstance.getElementById(dishId);
    if (node && node.length > 0) {
      const exactRoute = highlightDishRoute(node);
      if (exactRoute) {
        cyInstance.fit(exactRoute, 60);
      }
    }
  }
}

async function inspectDish(id) {
  const card = document.getElementById('dishStoryCard');
  if (!card) return;
  card.classList.remove('hidden');

  try {
    const item = await Api.getMenuItem(id);

    const img = document.getElementById('storyImg');
    if (img) img.src = getDishImg(item.id);

    document.getElementById('storyTitle').textContent = item.name;
    document.getElementById('storyPrice').textContent = '₹' + item.price;
    document.getElementById('storyCat').textContent = (item.categoryName || 'Main Dish') + ' • ' + (item.isVeg ? '🟢 Vegetarian' : '🔴 Non-Vegetarian');
    document.getElementById('storyDesc').textContent = item.description || 'Authentic Indian dish prepared fresh with hand-picked spices.';

    // Ingredients
    const ings = (item.ingredients || []).filter(i => i.name).map(i => `<span class="ing-chip">🌿 ${i.name}</span>`).join('');
    document.getElementById('storyIngredients').innerHTML = ings || '<span style="color:var(--muted);font-size:0.85rem">Standard spices & oil</span>';

    // Allergens
    const algs = (item.allergens || []).filter(a => a).map(a => `<span class="allergen-badge">⚠️ ${a}</span>`).join('');
    document.getElementById('storyAllergens').innerHTML = algs || '<span style="color:var(--muted);font-size:0.85rem">No major allergens listed</span>';

    // Pairings
    const pairs = (item.pairsWith || []).filter(p => p && p.name).map(p =>
      `<span style="display:inline-flex;align-items:center;background:var(--card);border:1px solid var(--border);padding:4px 10px;border-radius:8px;font-weight:700;font-size:0.82rem;">🍽️ ${p.name} (₹${p.price})</span>`
    ).join('');
    document.getElementById('storyPairs').innerHTML = pairs || '<span style="color:var(--muted);font-size:0.85rem">Naan, Lassi & Desserts</span>';

  } catch (err) {
    console.error('Inspect error:', err);
  }
}

function clearDishInspection() {
  const card = document.getElementById('dishStoryCard');
  if (card) card.classList.add('hidden');

  const sel = document.getElementById('dishGraphSelect');
  if (sel) sel.value = '';

  if (cyInstance) {
    cyInstance.elements().removeClass('highlighted faded');
    cyInstance.resize();
    cyInstance.fit(null, 40);
  }
}

// ── Search: highlight dish and its connected nodes ──
function graphFilterByDish(query) {
  if (!cyInstance) return;
  const q = query.trim().toLowerCase();

  if (!q) {
    cyInstance.elements().removeClass('highlighted faded');
    return;
  }

  // Find nodes whose label matches the search query (word boundary)
  const safeQ = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const qRegex = new RegExp('\\b' + safeQ, 'i');
  const matchedNodes = cyInstance.nodes().filter(node =>
    node.data('label') && qRegex.test(node.data('label'))
  );

  if (matchedNodes.length === 0) {
    cyInstance.elements().removeClass('highlighted faded');
    return;
  }

  // Include immediate neighbours (ingredients, allergens, categories)
  const neighbourhood = matchedNodes.closedNeighborhood();

  // Fade all, then highlight matched cluster
  cyInstance.elements().addClass('faded').removeClass('highlighted');
  neighbourhood.removeClass('faded').addClass('highlighted');

  // Zoom into matched region
  cyInstance.fit(neighbourhood, 80);
}

// ── Fallback list view (if Cytoscape.js not loaded) ──
function renderFallbackGraphList(canvas, data) {
  const nodes       = data.nodes || [];
  const menuItems   = nodes.filter(n => n.data.type === 'menuitem');
  const ingredients = nodes.filter(n => n.data.type === 'ingredient');
  const categories  = nodes.filter(n => n.data.type === 'category');
  const allergens   = nodes.filter(n => n.data.type === 'allergen');

  canvas.innerHTML = `
    <div style="padding:24px;overflow-y:auto;height:100%">
      <h3 style="color:var(--gold);margin-bottom:4px">🌐 Dish Connection Map (${nodes.length} Nodes)</h3>
      <p style="color:var(--muted);font-size:0.85rem;margin-bottom:20px">Graph library not loaded — showing list view</p>
      <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:16px">
        <div>
          <div style="color:#e85d04;font-weight:700;margin-bottom:8px">🍽️ Dishes (${menuItems.length})</div>
          <div style="display:flex;flex-wrap:wrap;gap:6px">
            ${menuItems.map(n => `<div style="background:rgba(232,93,4,0.12);border:1px solid rgba(232,93,4,0.3);padding:5px 10px;border-radius:8px;font-size:0.82rem">${n.data.label}</div>`).join('')}
          </div>
        </div>
        <div>
          <div style="color:#22c55e;font-weight:700;margin-bottom:8px">🌿 Ingredients (${ingredients.length})</div>
          <div style="display:flex;flex-wrap:wrap;gap:6px">
            ${ingredients.map(n => `<div style="background:rgba(34,197,94,0.1);border:1px solid rgba(34,197,94,0.25);padding:5px 10px;border-radius:8px;font-size:0.82rem">${n.data.label}</div>`).join('')}
          </div>
        </div>
        <div>
          <div style="color:#6366f1;font-weight:700;margin-bottom:8px">📂 Categories (${categories.length})</div>
          <div style="display:flex;flex-wrap:wrap;gap:6px">
            ${categories.map(n => `<div style="background:rgba(99,102,241,0.12);border:1px solid rgba(99,102,241,0.3);padding:5px 10px;border-radius:8px;font-size:0.82rem">${n.data.label}</div>`).join('')}
          </div>
        </div>
        <div>
          <div style="color:#ef4444;font-weight:700;margin-bottom:8px">⚠️ Allergens (${allergens.length})</div>
          <div style="display:flex;flex-wrap:wrap;gap:6px">
            ${allergens.map(n => `<div style="background:rgba(239,68,68,0.1);border:1px solid rgba(239,68,68,0.25);padding:5px 10px;border-radius:8px;font-size:0.82rem">${n.data.label}</div>`).join('')}
          </div>
        </div>
      </div>
    </div>
  `;
}

// ── Controls ──
function graphFitView() {
  if (cyInstance) {
    cyInstance.elements().removeClass('highlighted faded');
    cyInstance.resize();
    cyInstance.fit(null, 40);
  }
}

function graphReset() {
  clearDishInspection();
  if (cyInstance) {
    cyInstance.elements().removeClass('highlighted faded');
    const layout = cyInstance.layout({
      name: 'concentric',
      concentric: function(node) {
        const type = node.data('type');
        if (type === 'category')   return 4;
        if (type === 'menuitem')   return 3;
        if (type === 'ingredient') return 2;
        return 1;
      },
      levelWidth: function() { return 1; },
      padding: 60,
      animate: true,
      animationDuration: 400,
      fit: true
    });
    layout.run();
  }
}
