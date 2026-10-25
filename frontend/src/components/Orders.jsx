import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import API, { errorMessage } from "../axios";
import { formatDateTime, formatPrice } from "../utils/format";

const Orders = () => {
  const location = useLocation();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    API.get("/orders")
      .then(({ data }) => {
        if (!cancelled) setOrders(data);
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page-container">
      <h2 className="mb-3">My orders</h2>
      {location.state?.placed && (
        <div className="alert alert-success">Thank you! Your order has been placed.</div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <p>Loading...</p>}
      {!loading && !error && orders.length === 0 && (
        <div className="panel">
          <p className="mb-2">You have not placed any orders yet.</p>
          <Link to="/">Start shopping</Link>
        </div>
      )}
      {orders.map((order) => (
        <div className="panel mb-3" key={order.id}>
          <div className="d-flex justify-content-between flex-wrap mb-3">
            <div>
              <strong>Order #{order.id}</strong>
              <div className="small opacity-75">{formatDateTime(order.createdAt)}</div>
            </div>
            <span className="badge bg-success align-self-start">{order.status}</span>
          </div>
          <table className="table table-sm order-table mb-2">
            <thead>
              <tr>
                <th>Item</th>
                <th className="text-end">Price</th>
                <th className="text-end">Qty</th>
                <th className="text-end">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((item, index) => (
                <tr key={`${order.id}-${index}`}>
                  <td>{item.productName}</td>
                  <td className="text-end">{formatPrice(item.unitPrice)}</td>
                  <td className="text-end">{item.quantity}</td>
                  <td className="text-end">{formatPrice(item.lineTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="text-end fw-bold">Total: {formatPrice(order.totalAmount)}</div>
        </div>
      ))}
    </div>
  );
};

export default Orders;
