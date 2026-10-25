import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import API from "../axios";
import AppContext from "../Context/Context";
import unplugged from "../assets/unplugged.png";
import { formatPrice } from "../utils/format";

const PAGE_SIZE = 8;

const SORT_OPTIONS = [
  { value: "id,asc", label: "Default" },
  { value: "name,asc", label: "Name: A to Z" },
  { value: "price,asc", label: "Price: Low to High" },
  { value: "price,desc", label: "Price: High to Low" },
];

const Home = ({ selectedCategory }) => {
  const { addToCart } = useContext(AppContext);
  const [products, setProducts] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sort, setSort] = useState(SORT_OPTIONS[0].value);
  const [loading, setLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  // The page resets to the first one whenever the category changes, without an extra effect or fetch.
  const [pageState, setPageState] = useState({ category: selectedCategory, page: 0 });
  const page = pageState.category === selectedCategory ? pageState.page : 0;
  const goToPage = (target) => setPageState({ category: selectedCategory, page: target });

  const changeSort = (value) => {
    setSort(value);
    goToPage(0);
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    API.get("/products", {
      params: { page, size: PAGE_SIZE, sort, category: selectedCategory || undefined },
    })
      .then(({ data }) => {
        if (cancelled) return;
        setProducts(data.content);
        setTotalPages(data.page.totalPages);
        setTotalElements(data.page.totalElements);
        setIsError(false);
      })
      .catch((error) => {
        console.error("Error fetching products:", error);
        if (!cancelled) setIsError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page, sort, selectedCategory]);

  if (isError) {
    return (
      <div className="text-center" style={{ padding: "12rem 1rem" }}>
        <img src={unplugged} alt="Error" style={{ width: "100px", height: "100px" }} />
        <p className="mt-3">Could not load products. Is the backend running?</p>
      </div>
    );
  }

  // Show at most 5 page buttons around the current page.
  const firstVisible = Math.max(0, Math.min(page - 2, totalPages - 5));
  const visiblePages = Array.from(
    { length: Math.min(5, totalPages) },
    (_, i) => firstVisible + i
  );

  return (
    <>
      <div
        className="d-flex justify-content-between align-items-center flex-wrap"
        style={{ marginTop: "64px", padding: "20px 20px 0" }}
      >
        <h4 className="mb-0 products-heading">
          {selectedCategory || "All products"}
          <small className="ms-2 opacity-75">({totalElements})</small>
        </h4>
        <div className="d-flex align-items-center gap-2">
          <label htmlFor="sort" className="products-heading mb-0">Sort by</label>
          <select
            id="sort"
            className="form-select form-select-sm"
            style={{ width: "auto" }}
            value={sort}
            onChange={(e) => changeSort(e.target.value)}
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div
        className="grid"
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
          gap: "20px",
          padding: "20px",
          opacity: loading ? 0.6 : 1,
        }}
      >
        {!loading && products.length === 0 ? (
          <h2
            className="text-center products-heading"
            style={{ display: "flex", justifyContent: "center", alignItems: "center" }}
          >
            No Products Available
          </h2>
        ) : (
          products.map((product) => {
            const { id, brand, name, price, productAvailable } = product;
            return (
              <div
                className="card mb-3"
                style={{
                  width: "250px",
                  height: "270px",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.1)",
                  borderRadius: "10px",
                  overflow: "hidden",
                  backgroundColor: productAvailable ? "#fff" : "#ccc",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "flex-start",
                  alignItems: "stretch",
                }}
                key={id}
              >
                <Link
                  to={`/product/${id}`}
                  style={{ textDecoration: "none", color: "inherit" }}
                >
                  <div
                    className="card-body"
                    style={{
                      flexGrow: 1,
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "space-between",
                      padding: "10px",
                    }}
                  >
                    <div>
                      <h5
                        className="card-title"
                        style={{ margin: "0 0 10px 0", fontSize: "1.2rem" }}
                      >
                        {name.toUpperCase()}
                      </h5>
                      <i
                        className="card-brand"
                        style={{ fontStyle: "italic", fontSize: "0.8rem" }}
                      >
                        {"~ " + brand}
                      </i>
                    </div>
                    <hr className="hr-line" style={{ margin: "10px 0" }} />
                    <div className="home-cart-price">
                      <h5
                        className="card-text"
                        style={{ fontWeight: "600", fontSize: "1.1rem", marginBottom: "5px" }}
                      >
                        {formatPrice(price)}
                      </h5>
                    </div>
                    <button
                      className="btn-hover color-9"
                      style={{ margin: "10px 25px 0px " }}
                      onClick={(e) => {
                        e.preventDefault();
                        addToCart(product);
                      }}
                      disabled={!productAvailable || product.stockQuantity < 1}
                    >
                      {productAvailable && product.stockQuantity > 0 ? "Add to Cart" : "Out of Stock"}
                    </button>
                  </div>
                </Link>
              </div>
            );
          })
        )}
      </div>

      {totalPages > 1 && (
        <nav aria-label="Product pages" className="d-flex justify-content-center pb-4">
          <ul className="pagination mb-0">
            <li className={`page-item ${page === 0 ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => goToPage(page - 1)}>
                Previous
              </button>
            </li>
            {visiblePages.map((p) => (
              <li key={p} className={`page-item ${p === page ? "active" : ""}`}>
                <button className="page-link" onClick={() => goToPage(p)}>
                  {p + 1}
                </button>
              </li>
            ))}
            <li className={`page-item ${page >= totalPages - 1 ? "disabled" : ""}`}>
              <button className="page-link" onClick={() => goToPage(page + 1)}>
                Next
              </button>
            </li>
          </ul>
        </nav>
      )}
    </>
  );
};

export default Home;
