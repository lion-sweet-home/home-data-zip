import UserInfoTest from './components/UserInfoTest';
import MonthlyTop3Section from './components/monthly_top3_section';
import ApartmentSearch from './components/apartment_search';
import RecommendedApartmentsSection from './components/recommended_apartments_section';

export default function Home() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="px-6 md:px-10 pt-6">
        <div className="max-w-7xl mx-auto">
          <ApartmentSearch />
        </div>
      </div>
      <RecommendedApartmentsSection />
      <MonthlyTop3Section />
      <UserInfoTest />
    </div>
  );
}
