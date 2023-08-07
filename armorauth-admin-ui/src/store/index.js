import {createStore} from 'vuex'
import VuePersistence from 'vuex-persistedstate'

export default createStore({
    state: {},
    mutations: {},
    actions: {},
    plugins: [new VuePersistence()]
})