import { Routes, Route } from 'react-router-dom';
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

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/posts" element={<Posts />} />
        <Route path="/posts/:id" element={<PostDetail />} />
        <Route path="/errand" element={<Errand />} />
        <Route path="/errand/:id" element={<ErrandDetail />} />
        <Route path="/market" element={<Market />} />
        <Route path="/market/:id" element={<MarketDetail />} />
        <Route path="/hot" element={<Hot />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/messages" element={<Messages />} />
      </Route>
    </Routes>
  );
}

export default App;
