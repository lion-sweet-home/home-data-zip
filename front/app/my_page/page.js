'use client';

import UserInfoCard from './components/info/user_info';
import PasswordCard from './components/info/password';
import SettingCard from './components/info/setting';
import FavoriteCard from './components/favorite';
import ChatCard from './components/chat';
import SubscriptionCard from './components/subscription';

export default function MyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-6 md:px-10 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">마이페이지</h1>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left side */}
          <div className="lg:col-span-4 space-y-6">
            <UserInfoCard />
            <PasswordCard />
            <SettingCard />
          </div>

          {/* Right side */}
          <div className="lg:col-span-8 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <FavoriteCard />
              <ChatCard />
            </div>
            <SubscriptionCard />
          </div>
        </div>
      </div>
    </div>
  );
}

