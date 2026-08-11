<script setup>
import AccountOverview from '@/components/user/AccountOverview.vue'
import ProfilePanel from '@/components/user/ProfilePanel.vue'
import RecentActivity from '@/components/user/RecentActivity.vue'
import SecurityPanel from '@/components/user/SecurityPanel.vue'
</script>

<template>
  <div class="profile-view">
    <AccountOverview />
    <RecentActivity />
    <ProfilePanel />
    <SecurityPanel />
  </div>
</template>

<style scoped>
.profile-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
