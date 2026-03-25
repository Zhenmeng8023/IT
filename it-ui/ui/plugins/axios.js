export default function({ $axios, store }) {
  $axios.defaults.baseURL = 'http://39.102.72.27:18080/'
  
  $axios.interceptors.request.use(config => {
    const token = store.getters['user/getToken']
    if (token) {
      config.headers['X-Token'] = token
    }
    return config
  })
}