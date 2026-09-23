import { Routes, Route, useParams } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import Home from './pages/Home';
import Posts from './pages/Posts';
import PostDetail from './pages/PostDetail';
import Errand from './pages/Errand';
import ErrandDetail from './pages/ErrandDetail';
import Market from './pages/Market';
import MarketDetail from './pages/MarketDetail';
import Hot from './pages/Hot';
import Login from './pages/Login';
import Profile from './pages/Profile';
import Messages from './pages/Messages';

// 资源或会话变化时隔离局部表单及未完成写操作，避免旧结果污染新页面。
function ResourcePage({ component: Component }) {
  const { id } = useParams();
  const { token } = useAuth();
  return <Component key={`${id || ''}:${token || 'guest'}`} />;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/posts" element={<ResourcePage component={Posts} />} />
        <Route path="/posts/:id" element={<ResourcePage component={PostDetail} />} />
        <Route path="/errand" element={<ResourcePage component={Errand} />} />
        <Route path="/errand/:id" element={<ResourcePage component={ErrandDetail} />} />
        <Route path="/market" element={<ResourcePage component={Market} />} />
        <Route path="/market/:id" element={<ResourcePage component={MarketDetail} />} />
        <Route path="/hot" element={<Hot />} />
        <Route path="/profile" element={<ResourcePage component={Profile} />} />
        <Route path="/messages" element={<ResourcePage component={Messages} />} />
      </Route>
    </Routes>
  );
}

export default App;
