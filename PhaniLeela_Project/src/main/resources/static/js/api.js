// API client — all calls to the Spring Boot backend
const API_BASE = '';

const Api = {
  async get(path) {
    const res = await fetch(API_BASE + path);
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Network error' }));
      throw new Error(err.message || `HTTP ${res.status}`);
    }
    return res.json();
  },

  async post(path, body) {
    const res = await fetch(API_BASE + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Network error' }));
      throw new Error(err.message || `HTTP ${res.status}`);
    }
    return res.json();
  },

  async put(path, body) {
    const res = await fetch(API_BASE + path, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Network error' }));
      throw new Error(err.message || `HTTP ${res.status}`);
    }
    return res.json();
  },

  async delete(path) {
    const res = await fetch(API_BASE + path, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Network error' }));
      throw new Error(err.message || `HTTP ${res.status}`);
    }
    return res.json();
  },

  getMenu:           () => Api.get('/api/menu'),
  getMenuItem:       (id) => Api.get(`/api/menu/${id}`),
  getMenuByCategory: (catId) => Api.get(`/api/menu/category/${catId}`),
  getMenuSafe:       (allergens) => Api.get(`/api/menu/safe?allergens=${allergens.join('&allergens=')}`),
  getCategories:     () => Api.get('/api/categories'),
  getStats:          () => Api.get('/api/stats'),
  getOrders:         () => Api.get('/api/orders'),
  placeOrder:        (body) => Api.post('/api/orders', body),
  updateOrderStatus: (id, status) => Api.put(`/api/orders/${id}/status`, { status }),
  updatePaymentStatus: (id, paymentStatus) => Api.put(`/api/orders/${id}/payment`, { paymentStatus }),
  deleteOrder:       (id) => Api.delete(`/api/orders/${id}`),
  deleteAllOrders:   () => Api.delete('/api/orders'),
  getGraph:          () => Api.get('/api/graph'),
};
