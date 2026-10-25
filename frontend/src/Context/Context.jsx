import { useState, useEffect, useCallback, useMemo, createContext } from "react";

const AppContext = createContext({
  cart: [],
  addToCart: () => {},
  removeFromCart: () => {},
  updateQuantity: () => {},
  replaceCart: () => {},
  clearCart: () => {},
});

const readStoredCart = () => {
  try {
    return JSON.parse(localStorage.getItem("cart")) || [];
  } catch {
    return [];
  }
};

// The cart is only a local basket. Stock is checked and the order is stored by the backend at checkout.
export const AppProvider = ({ children }) => {
  const [cart, setCart] = useState(readStoredCart);

  useEffect(() => {
    localStorage.setItem("cart", JSON.stringify(cart));
  }, [cart]);

  // These only use functional state updates, so they can keep a stable identity across renders.
  const addToCart = useCallback((product) => {
    setCart((current) => {
      const existing = current.find((item) => item.id === product.id);
      if (existing) {
        return current.map((item) =>
          item.id === product.id
            ? { ...item, quantity: Math.min(item.quantity + 1, product.stockQuantity) }
            : item
        );
      }
      return [
        ...current,
        {
          id: product.id,
          name: product.name,
          brand: product.brand,
          price: product.price,
          stockQuantity: product.stockQuantity,
          quantity: 1,
        },
      ];
    });
  }, []);

  const removeFromCart = useCallback((productId) => {
    setCart((current) => current.filter((item) => String(item.id) !== String(productId)));
  }, []);

  const updateQuantity = useCallback((productId, quantity) => {
    setCart((current) =>
      current.map((item) => (item.id === productId ? { ...item, quantity } : item))
    );
  }, []);

  const replaceCart = useCallback((items) => setCart(items), []);

  const clearCart = useCallback(() => setCart([]), []);

  const value = useMemo(
    () => ({ cart, addToCart, removeFromCart, updateQuantity, replaceCart, clearCart }),
    [cart, addToCart, removeFromCart, updateQuantity, replaceCart, clearCart]
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export default AppContext;
