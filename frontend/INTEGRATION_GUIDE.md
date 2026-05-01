# PhoneShop Frontend Pages - Navigation Guide

## How to Link These New Pages

### From **checkout.html** → **payment.html**
```javascript
// Change the "Đặt hàng" button to navigate to payment first
onclick="navigate('payment.html')"
```

### From **orders.html** → **tracking.html**
```javascript
// Modify order card onclick to show tracking
onclick="navigate('tracking.html',{id:${o.orderId}})"
```

### From **order-detail.html** → **order-cancel.html**
Add cancel button in order detail:
```html
<button class="btn btn-danger" onclick="navigate('order-cancel.html',{id:${order.orderId}})">
  <i class="fas fa-times"></i> Hủy đơn hàng
</button>
```

### From **order-detail.html** → **review.html**
For each item in delivered orders:
```html
<button class="btn btn-primary btn-sm" onclick="navigate('review.html',{orderId:${order.orderId},itemId:${item.itemId}})">
  <i class="fas fa-star"></i> Viết đánh giá
</button>
```

### From **profile.html** → **customer-support.html**
Add menu item:
```html
<div class="menu-item" onclick="navigate('customer-support.html')">
  <i class="fas fa-headset"></i> Hỗ trợ khách hàng 
  <span class="arrow"><i class="fas fa-chevron-right"></i></span>
</div>
```

## URL Parameters Reference

| Page | Parameters | Example |
|------|-----------|---------|
| tracking.html | `id` (orderId) | `tracking.html?id=123` |
| order-cancel.html | `id` (orderId) | `order-cancel.html?id=123` |
| review.html | `orderId`, `itemId` | `review.html?orderId=123&itemId=456` |
| customer-support.html | None | `customer-support.html` |
| payment.html | None | `payment.html` |

## Page Flow Diagram

```
index.html
  ↓
products.html → product-detail.html
  ↓               ↓
cart.html → checkout.html → payment.html ✅
             (NEW: redirects to payment)
                ↓
            orders.html → order-detail.html
              ↓(click order)   ↓
              ↓         ┌─────┴──────────┐
         tracking.html✅  order-cancel.html✅
                         ↓
                    review.html✅

profile.html → customer-support.html✅
(add menu item)
```

## Testing Steps

1. **Test Payment Flow**:
   - Go to checkout → Should proceed to payment.html
   - Try all payment methods
   - Verify order summary displays correctly

2. **Test Order Tracking**:
   - Create an order
   - View orders list
   - Click on order → Should show tracking.html
   - Check timeline displays all statuses

3. **Test Order Cancellation**:
   - Open order detail
   - Click "Hủy đơn hàng" button
   - Try cancelling with different reasons
   - Verify form validation works

4. **Test Product Reviews**:
   - Go to orders → Delivered orders
   - Click "Viết đánh giá"
   - Test star rating, text input, image upload
   - Submit review

5. **Test Customer Support**:
   - Open support page
   - Submit a new ticket
   - Verify ticket appears in history
   - Click ticket to see detail and reply

## CSS Styling Notes

All pages use the existing `assets/css/style.css`:
- No new CSS files needed
- Inline styles used for page-specific layouts
- Responsive breakpoints at 768px for desktop
- Mobile-first design with bottom navigation

## Mobile Navigation

All pages include mobile-friendly elements:
- ✅ Header with back button
- ✅ Bottom navigation bar
- ✅ Touch-friendly button sizes
- ✅ Optimized form inputs for mobile
- ✅ Scrollable content areas
- ✅ Proper viewport meta tags

## Browser Compatibility

- Modern browsers (Chrome, Firefox, Safari, Edge)
- Mobile browsers (iOS Safari, Chrome Android)
- Uses ES6+ JavaScript features
- No IE11 support needed

## Performance Notes

- Lazy load images with placeholders
- Use spinners during API calls
- Cache user data in localStorage (via api.js)
- Minimize re-renders in JavaScript
- Optimize image sizes before upload

## Next Steps

1. Link these pages from existing pages as shown above
2. Implement backend API endpoints matching the endpoint list
3. Test all flows end-to-end
4. Deploy frontend to server
5. Configure API base URL in `assets/js/api.js`
