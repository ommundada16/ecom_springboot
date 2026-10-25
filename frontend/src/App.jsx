import "./App.css";
import { useState } from "react";
import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import { AppProvider } from "./Context/Context";
import { AuthProvider } from "./Context/AuthContext";
import Home from "./components/Home";
import Navbar from "./components/Navbar";
import Cart from "./components/Cart";
import AddProduct from "./components/AddProduct";
import UpdateProduct from "./components/UpdateProduct";
import Product from "./components/Product";
import Login from "./components/Login";
import Register from "./components/Register";
import Orders from "./components/Orders";
import ProtectedRoute from "./components/ProtectedRoute";

const NotFound = () => (
  <div className="page-container narrow">
    <div className="panel text-center">
      <h2>Page not found</h2>
      <Link to="/">Back to the shop</Link>
    </div>
  </div>
);

function App() {
  const [selectedCategory, setSelectedCategory] = useState("");

  return (
    <AuthProvider>
      <AppProvider>
        <BrowserRouter>
          <Navbar onSelectCategory={setSelectedCategory} />
          <Routes>
            <Route path="/" element={<Home selectedCategory={selectedCategory} />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/product/:id" element={<Product />} />
            <Route path="/cart" element={<Cart />} />
            <Route
              path="/orders"
              element={
                <ProtectedRoute>
                  <Orders />
                </ProtectedRoute>
              }
            />
            <Route
              path="/add_product"
              element={
                <ProtectedRoute adminOnly>
                  <AddProduct />
                </ProtectedRoute>
              }
            />
            <Route
              path="/product/update/:id"
              element={
                <ProtectedRoute adminOnly>
                  <UpdateProduct />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </AppProvider>
    </AuthProvider>
  );
}

export default App;
