export default {
  namespaced: true,
  
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || '',
    roles: JSON.parse(localStorage.getItem('roles') || '[]')
  }),
  
  getters: {
    isLoggedIn: state => !!state.token,
    hasRole: state => roleName => state.roles.includes(roleName)
  },
  
  mutations: {
    setUser(state, user) {
      state.user = user
    },
    setToken(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    setRoles(state, roles) {
      state.roles = roles
      localStorage.setItem('roles', JSON.stringify(roles))
    },
    clearAuth(state) {
      state.user = null
      state.token = ''
      state.roles = []
      localStorage.removeItem('token')
      localStorage.removeItem('roles')
    }
  },
  
  actions: {
    login({ commit }, { user, token, roles }) {
      commit('setUser', user)
      commit('setToken', token)
      commit('setRoles', roles)
    },
    logout({ commit }) {
      commit('clearAuth')
    }
  }
}