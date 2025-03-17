<script setup>
// App component now just serves as a router container
</script>

<template>
  <router-view />
</template>

<style>
@import './assets/base.css';

body {
  margin: 0;
  padding: 0;
  font-family: 'Roboto', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}
</style>
