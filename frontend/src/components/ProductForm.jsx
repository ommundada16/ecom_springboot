import { useState } from "react";
import categories from "../categories";
import { errorMessage } from "../axios";

const emptyProduct = {
  name: "",
  brand: "",
  description: "",
  price: "",
  category: "",
  stockQuantity: "",
  releaseDate: "",
  productAvailable: false,
};

const ProductForm = ({ initialValues = emptyProduct, onSubmit, title, submitLabel }) => {
  const [product, setProduct] = useState(initialValues);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Keep a product's existing category selectable even if it is not one of the standard ones.
  const categoryOptions =
    !product.category || categories.includes(product.category)
      ? categories
      : [product.category, ...categories];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setProduct({ ...product, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await onSubmit({ ...product, releaseDate: product.releaseDate || null });
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  return (
    <div className="page-container">
      <div className="panel">
        <h2 className="mb-3">{title}</h2>
        {error && <div className="alert alert-danger">{error}</div>}
        <form className="row g-3" onSubmit={handleSubmit}>
          <div className="col-md-6">
            <label className="form-label" htmlFor="name">Name</label>
            <input
              id="name"
              type="text"
              className="form-control"
              placeholder="Product name"
              onChange={handleInputChange}
              value={product.name}
              name="name"
              required
            />
          </div>
          <div className="col-md-6">
            <label className="form-label" htmlFor="brand">Brand</label>
            <input
              id="brand"
              type="text"
              name="brand"
              className="form-control"
              placeholder="Brand"
              value={product.brand}
              onChange={handleInputChange}
            />
          </div>
          <div className="col-12">
            <label className="form-label" htmlFor="description">Description</label>
            <input
              id="description"
              type="text"
              className="form-control"
              placeholder="Product description"
              value={product.description}
              name="description"
              onChange={handleInputChange}
            />
          </div>
          <div className="col-md-4">
            <label className="form-label" htmlFor="price">Price (₹)</label>
            <input
              id="price"
              type="number"
              min="0.01"
              step="0.01"
              className="form-control"
              placeholder="Eg: 1000"
              onChange={handleInputChange}
              value={product.price}
              name="price"
              required
            />
          </div>
          <div className="col-md-4">
            <label className="form-label" htmlFor="category">Category</label>
            <select
              id="category"
              className="form-select"
              value={product.category}
              onChange={handleInputChange}
              name="category"
            >
              <option value="">Select category</option>
              {categoryOptions.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label" htmlFor="stockQuantity">Stock quantity</label>
            <input
              id="stockQuantity"
              type="number"
              min="0"
              step="1"
              className="form-control"
              placeholder="Units in stock"
              onChange={handleInputChange}
              value={product.stockQuantity}
              name="stockQuantity"
              required
            />
          </div>
          <div className="col-md-4">
            <label className="form-label" htmlFor="releaseDate">Release date</label>
            <input
              id="releaseDate"
              type="date"
              className="form-control"
              value={product.releaseDate}
              name="releaseDate"
              onChange={handleInputChange}
            />
          </div>
          <div className="col-12">
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                name="productAvailable"
                id="productAvailable"
                checked={product.productAvailable}
                onChange={(e) => setProduct({ ...product, productAvailable: e.target.checked })}
              />
              <label className="form-check-label" htmlFor="productAvailable">
                Product available
              </label>
            </div>
          </div>
          <div className="col-12">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? "Saving..." : submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProductForm;
