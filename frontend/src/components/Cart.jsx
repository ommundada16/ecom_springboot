import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppContext from "../Context/Context";
import { useAuth } from "../Context/AuthContext";
import API, { errorMessage } from "../axios";
import CheckoutPopup from "./CheckoutPopup";
import { Button } from "react-bootstrap";
import { formatPrice } from "../utils/format";

const Cart = () => {
  const { cart, removeFromCart, updateQuantity, replaceCart, clearCart } = useContext(AppContext);
  const { user } = useAuth();
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [checkoutError, setCheckoutError] = useState("");
  const [notice, setNotice] = useState("");

  const cartRef = useRef(cart);
  cartRef.current = cart;

  // A sync that finishes after the page was left or the order was placed must not touch the cart.
  const active = useRef(true);
  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
    };
  }, []);

  const totalPrice = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  // Refresh price and stock from the backend and drop items that are no longer for sale.
  const syncWithBackend = useCallback(async () => {
    const items = cartRef.current;
    if (items.length === 0) return;
    const results = await Promise.allSettled(items.map((item) => API.get(`/products/${item.id}`)));
    if (!active.current) return;
    const kept = [];
    const removed = [];
    results.forEach((result, index) => {
      const item = items[index];
      if (result.status === "fulfilled") {
        const product = result.value.data;
        if (product.productAvailable && product.stockQuantity > 0) {
          kept.push({
            ...item,
            name: product.name,
            brand: product.brand,
            price: product.price,
            stockQuantity: product.stockQuantity,
            quantity: Math.min(item.quantity, product.stockQuantity),
          });
        } else {
          removed.push(item.name);
        }
      } else if (result.reason.response?.status === 404) {
        removed.push(item.name);
      } else {
        kept.push(item);
      }
    });
    replaceCart(kept);
    if (removed.length > 0) {
      setNotice(`Removed because they are no longer available: ${removed.join(", ")}.`);
    }
  }, [replaceCart]);

  useEffect(() => {
    syncWithBackend();
  }, [syncWithBackend]);

  const handleIncrease = (item) => {
    if (item.quantity >= item.stockQuantity) {
      setNotice(`Only ${item.stockQuantity} unit(s) of ${item.name} in stock.`);
      return;
    }
    updateQuantity(item.id, item.quantity + 1);
  };

  const handleDecrease = (item) => {
    updateQuantity(item.id, Math.max(item.quantity - 1, 1));
  };

  const handleCheckoutClick = () => {
    if (!user) {
      navigate("/login", { state: { from: "/cart" } });
      return;
    }
    setCheckoutError("");
    setShowModal(true);
  };

  const handleCheckout = async () => {
    setSubmitting(true);
    setCheckoutError("");
    try {
      await API.post("/orders", {
        items: cart.map((item) => ({ productId: item.id, quantity: item.quantity })),
      });
      active.current = false;
      clearCart();
      setShowModal(false);
      navigate("/orders", { state: { placed: true } });
    } catch (err) {
      setCheckoutError(errorMessage(err));
      syncWithBackend();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="cart-container">
      <div className="shopping-cart">
        <div className="title">Shopping Bag</div>
        {notice && (
          <div className="alert alert-warning mt-3" role="alert">
            {notice}
          </div>
        )}
        {cart.length === 0 ? (
          <div className="empty" style={{ textAlign: "left", padding: "2rem" }}>
            <h4>Your cart is empty</h4>
          </div>
        ) : (
          <>
            {cart.map((item) => (
              <li key={item.id} className="cart-item">
                <div className="item" style={{ display: "flex", alignContent: "center" }}>
                  <div className="description">
                    <span>{item.brand}</span>
                    <span>{item.name}</span>
                  </div>

                  <div className="quantity">
                    <button
                      className="plus-btn"
                      type="button"
                      name="button"
                      onClick={() => handleIncrease(item)}
                    >
                      <i className="bi bi-plus-square-fill"></i>
                    </button>
                    <input type="button" name="name" value={item.quantity} readOnly />
                    <button
                      className="minus-btn"
                      type="button"
                      name="button"
                      onClick={() => handleDecrease(item)}
                    >
                      <i className="bi bi-dash-square-fill"></i>
                    </button>
                  </div>

                  <div className="total-price " style={{ textAlign: "center" }}>
                    {formatPrice(item.price * item.quantity)}
                  </div>
                  <button className="remove-btn" onClick={() => removeFromCart(item.id)}>
                    <i className="bi bi-trash3-fill"></i>
                  </button>
                </div>
              </li>
            ))}
            <div className="total">Total: {formatPrice(totalPrice)}</div>
            <Button
              className="btn btn-primary"
              style={{ width: "100%" }}
              onClick={handleCheckoutClick}
            >
              {user ? "Checkout" : "Log in to checkout"}
            </Button>
          </>
        )}
      </div>
      <CheckoutPopup
        show={showModal}
        handleClose={() => setShowModal(false)}
        cartItems={cart}
        totalPrice={totalPrice}
        handleCheckout={handleCheckout}
        submitting={submitting}
        error={checkoutError}
      />
    </div>
  );
};

export default Cart;
