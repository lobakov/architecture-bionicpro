import React from 'react';
import ReportPage from './components/ReportPage';
import { checkAuthentication, redirectToLogin } from './services/authService';

const App: React.FC = () => {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);

  useEffect(() => {
    checkAuthentication()
      .then(setAuthenticated)
      .catch(() => setAuthenticated(false));
  }, []);

  if (authenticated === null) {
    return <div>Loading...</div>;
  }

  return (
    <div className="App">
      <ReportPage
        authenticated={authenticated}
        onLogin={redirectToLogin}
      />
    </div>
  );
};

export default App;
