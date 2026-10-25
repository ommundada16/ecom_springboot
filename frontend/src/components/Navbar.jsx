import { useContext, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../axios";
import AppContext from "../Context/Context";
import { useAuth } from "../Context/AuthContext";
import categories from "../categories";

const Navbar = ({ onSelectCategory }) => {
  const navigate = useNavigate();
  const { user, isAdmin, logout } = useAuth();
  const { cart } = useContext(AppContext);

  const getInitialTheme = () => {
    const storedTheme = localStorage.getItem("theme");
    return storedTheme ? storedTheme : "light-theme";
  };
  const [theme, setTheme] = useState(getInitialTheme());
  const [input, setInput] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);

  const cartCount = cart.reduce((total, item) => total + item.quantity, 0);

  useEffect(() => {
    const keyword = input.trim();
    if (!keyword) {
      setSearchResults([]);
      setSearching(false);
      return;
    }
    setSearching(true);
    let cancelled = false;
    // Wait for a short pause in typing instead of calling the API on every keystroke.
    const timer = setTimeout(async () => {
      try {
        const { data } = await API.get("/products/search", { params: { keyword } });
        if (!cancelled) setSearchResults(data);
      } catch (error) {
        console.error("Error searching:", error);
        if (!cancelled) setSearchResults([]);
      } finally {
        if (!cancelled) setSearching(false);
      }
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [input]);

  const handleCategorySelect = (category) => {
    onSelectCategory(category);
    navigate("/");
  };

  const toggleTheme = () => {
    const newTheme = theme === "dark-theme" ? "light-theme" : "dark-theme";
    setTheme(newTheme);
    localStorage.setItem("theme", newTheme);
  };

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  useEffect(() => {
    document.body.className = theme;
  }, [theme]);

  const showSearchResults = input.trim().length > 0;

  return (
    <>
      <header>
        <nav className="navbar navbar-expand-lg fixed-top">
          <div className="container-fluid">
            <Link className="navbar-brand" to="/" onClick={() => onSelectCategory("")}>
              ShopEase
            </Link>
            <button
              className="navbar-toggler"
              type="button"
              data-bs-toggle="collapse"
              data-bs-target="#navbarSupportedContent"
              aria-controls="navbarSupportedContent"
              aria-expanded="false"
              aria-label="Toggle navigation"
            >
              <span className="navbar-toggler-icon"></span>
            </button>
            <div className="collapse navbar-collapse" id="navbarSupportedContent">
              <ul className="navbar-nav me-auto mb-2 mb-lg-0">
                <li className="nav-item">
                  <Link className="nav-link active" aria-current="page" to="/" onClick={() => onSelectCategory("")}>
                    Home
                  </Link>
                </li>
                {isAdmin && (
                  <li className="nav-item">
                    <Link className="nav-link" to="/add_product">
                      Add Product
                    </Link>
                  </li>
                )}
                {user && (
                  <li className="nav-item">
                    <Link className="nav-link" to="/orders">
                      My Orders
                    </Link>
                  </li>
                )}

                <li className="nav-item dropdown">
                  <a
                    className="nav-link dropdown-toggle"
                    href="/"
                    role="button"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                    onClick={(e) => e.preventDefault()}
                  >
                    Categories
                  </a>

                  <ul className="dropdown-menu">
                    <li>
                      <button className="dropdown-item" onClick={() => handleCategorySelect("")}>
                        All products
                      </button>
                    </li>
                    {categories.map((category) => (
                      <li key={category}>
                        <button
                          className="dropdown-item"
                          onClick={() => handleCategorySelect(category)}
                        >
                          {category}
                        </button>
                      </li>
                    ))}
                  </ul>
                </li>
              </ul>
              <button className="theme-btn" onClick={() => toggleTheme()}>
                {theme === "dark-theme" ? (
                  <i className="bi bi-moon-fill"></i>
                ) : (
                  <i className="bi bi-sun-fill"></i>
                )}
              </button>
              <div className="d-flex align-items-center cart">
                <Link to="/cart" className="nav-link text-dark">
                  <i
                    className="bi bi-cart me-2"
                    style={{ display: "flex", alignItems: "center" }}
                  >
                    Cart{cartCount > 0 ? ` (${cartCount})` : ""}
                  </i>
                </Link>
                <input
                  className="form-control me-2"
                  type="search"
                  placeholder="Search"
                  aria-label="Search"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                />
                {showSearchResults && (
                  <ul className="list-group">
                    {searchResults.length > 0 ? (
                      searchResults.map((result) => (
                        <li key={result.id} className="list-group-item">
                          <Link
                            to={`/product/${result.id}`}
                            className="search-result-link"
                            onClick={() => setInput("")}
                          >
                            <span>{result.name}</span>
                          </Link>
                        </li>
                      ))
                    ) : (
                      !searching && (
                        <p className="no-results-message">No products match your search</p>
                      )
                    )}
                  </ul>
                )}
              </div>
              <div className="d-flex align-items-center ms-3 gap-2 auth-links">
                {user ? (
                  <>
                    <span className="navbar-user">
                      Hi, {user.name.split(" ")[0]}
                      {isAdmin && <span className="badge bg-warning text-dark ms-1">Admin</span>}
                    </span>
                    <button className="btn btn-outline-secondary btn-sm" onClick={handleLogout}>
                      Logout
                    </button>
                  </>
                ) : (
                  <>
                    <Link className="btn btn-outline-secondary btn-sm" to="/login">
                      Log in
                    </Link>
                    <Link className="btn btn-primary btn-sm" to="/register">
                      Sign up
                    </Link>
                  </>
                )}
              </div>
            </div>
          </div>
        </nav>
      </header>
    </>
  );
};

export default Navbar;
