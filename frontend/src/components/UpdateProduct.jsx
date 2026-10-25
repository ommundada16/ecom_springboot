import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import API, { errorMessage } from "../axios";
import ProductForm from "./ProductForm";

const toFormValues = (product) => ({
  name: product.name ?? "",
  brand: product.brand ?? "",
  description: product.description ?? "",
  price: product.price ?? "",
  category: product.category ?? "",
  stockQuantity: product.stockQuantity ?? "",
  releaseDate: product.releaseDate ?? "",
  productAvailable: product.productAvailable,
});

const UpdateProduct = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [initialValues, setInitialValues] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    API.get(`/products/${id}`)
      .then(({ data }) => {
        if (!cancelled) setInitialValues(toFormValues(data));
      })
      .catch((err) => {
        console.error("Error fetching product:", err);
        if (!cancelled) setError(errorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleSubmit = async (product) => {
    await API.put(`/products/${id}`, product);
    navigate(`/product/${id}`);
  };

  if (error) {
    return (
      <div className="page-container narrow">
        <div className="alert alert-danger">{error}</div>
      </div>
    );
  }
  if (!initialValues) {
    return (
      <h2 className="text-center products-heading" style={{ padding: "10rem" }}>
        Loading...
      </h2>
    );
  }

  return (
    <ProductForm
      title="Update product"
      submitLabel="Save changes"
      initialValues={initialValues}
      onSubmit={handleSubmit}
    />
  );
};

export default UpdateProduct;
