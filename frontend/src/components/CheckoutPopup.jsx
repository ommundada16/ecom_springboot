import { Modal, Button } from "react-bootstrap";
import { formatPrice } from "../utils/format";

const CheckoutPopup = ({
  show,
  handleClose,
  cartItems,
  totalPrice,
  handleCheckout,
  submitting,
  error,
}) => {
  return (
    <div className="checkoutPopup">
      <Modal show={show} onHide={handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>Checkout</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {error && <div className="alert alert-danger">{error}</div>}
          <div className="checkout-items">
            {cartItems.map((item) => (
              <div
                key={item.id}
                className="checkout-item"
                style={{ display: "flex", marginBottom: "10px" }}
              >
                <div>
                  <b>
                    <p className="mb-1">{item.name}</p>
                  </b>
                  <p className="mb-1">Quantity: {item.quantity}</p>
                  <p className="mb-1">Price: {formatPrice(item.price * item.quantity)}</p>
                </div>
              </div>
            ))}
            <div>
              <h5
                style={{
                  color: "black",
                  display: "flex",
                  justifyContent: "center",
                  fontSize: "1.3rem",
                  fontWeight: "bold",
                }}
              >
                Total: {formatPrice(totalPrice)}
              </h5>
            </div>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleClose} disabled={submitting}>
            Close
          </Button>
          <Button variant="primary" onClick={handleCheckout} disabled={submitting}>
            {submitting ? "Placing order..." : "Confirm Purchase"}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default CheckoutPopup;
