import { useNavigate } from "react-router-dom";
import API from "../axios";
import ProductForm from "./ProductForm";

const AddProduct = () => {
  const navigate = useNavigate();

  const handleSubmit = async (product) => {
    const { data } = await API.post("/products", product);
    navigate(`/product/${data.id}`);
  };

  return <ProductForm title="Add a product" submitLabel="Add product" onSubmit={handleSubmit} />;
};

export default AddProduct;
