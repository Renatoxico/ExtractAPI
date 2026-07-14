import { mount } from 'svelte';
import App from './App.svelte';
import './lib/firebase.js';

const app = mount(App, { target: document.getElementById('app') });

export default app;
