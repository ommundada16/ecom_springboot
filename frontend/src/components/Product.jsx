import { useNavigate, useParams } from "react-router-dom";
import { useContext, useEffect, useState } from "react";
import AppContext from "../Context/Context";
import { useAuth } from "../Context/AuthContext";
import API, { errorMessage } from "../axios";
import { formatDate, formatPrice } from "../utils/format";

const Product = () => {
  const { id } = useParams();
  const { addToCart, removeFromCart } = useContext(AppContext);
  const { isAdmin } = useAuth();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    setProduct(null);
    setError("");
    setNotice("");
    API.get(`/products/${id}`)
      .then(({ data }) => {
        if (!cancelled) setProduct(data);
      })
      .catch((err) => {
        console.error("Error fetching product:", err);
        if (!cancelled) {
          setError(err.response?.status === 404 ? "This product does not exist." : errorMessage(err));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const deleteProduct = async () => {
    if (!window.confirm(`Delete "${product.name}"? This cannot be undone.`)) return;
    try {
      await API.delete(`/products/${id}`);
      removeFromCart(id);
      navigate("/");
    } catch (err) {
      console.error("Error deleting product:", err);
      setError(errorMessage(err));
    }
  };

  const handleAddToCart = () => {
    addToCart(product);
    setNotice("Added to your cart.");
  };

  if (error) {
    return (
      <div className="page-container narrow">
        <div className="alert alert-danger">{error}</div>
      </div>
    );
  }
  if (!product) {
    return (
      <h2 className="text-center products-heading" style={{ padding: "10rem" }}>
        Loading...
      </h2>
    );
  }

  const inStock = product.productAvailable && product.stockQuantity > 0;

  return (
    <div className="page-container">
      <div className="panel">
        <div className="d-flex justify-content-between">
          <span style={{ fontSize: "1.2rem", fontWeight: "lighter" }}>{product.category}</span>
          <span>
            Listed: <i>{formatDate(product.releaseDate)}</i>
          </span>
        </div>

        <h1
          style={{ fontSize: "2rem", margin: "1rem 0 0.5rem", textTransform: "capitalize", letterSpacing: "1px" }}
        >
          {product.name}
        </h1>
        <i>{product.brand}</i>
        <p style={{ fontWeight: "bold", fontSize: "1rem", margin: "1.5rem 0 0" }}>PRODUCT DESCRIPTION:</p>
        <p style={{ marginBottom: "1.5rem" }}>{product.description}</p>

        <div className="d-flex align-items-center flex-wrap gap-3 mb-2">
          <span style={{ fontSize: "2rem", fontWeight: "bold" }}>{formatPrice(product.price)}</span>
          <button
            className="btn btn-primary"
            onClick={handleAddToCart}
            disabled={!inStock}
            style={{ padding: "0.8rem 2rem" }}
          >
            {inStock ? "Add to cart" : "Out of Stock"}
          </button>
        </div>
        {notice && <div className="text-success mb-2">{notice}</div>}
        <h6 className="mb-3">
          Stock available:{" "}
          <i style={{ color: inStock ? "green" : "crimson", fontWeight: "bold" }}>
            {product.stockQuantity}
          </i>
        </h6>

        {isAdmin && (
          <div className="d-flex gap-3 mt-3">
            <button
              className="btn btn-primary"
              type="button"
              onClick={() => navigate(`/product/update/${id}`)}
            >
              Update
            </button>
            <button className="btn btn-danger" type="button" onClick={deleteProduct}>
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Product;
