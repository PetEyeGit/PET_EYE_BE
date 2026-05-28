# Hướng dẫn Frontend tích hợp Goong Map

## 1. Cài đặt thư viện

```bash
npm install @goongmaps/goong-js
# hoặc
yarn add @goongmaps/goong-js
```

## 2. Cấu hình API Key

### Tạo file `.env` trong thư mục frontend:

```env
# Dùng Maptiles Key (không phải API Key)
VITE_GOONG_MAP_KEY=Qu7Vly4VMWH8W5pYa8X1TCjzozBR21AY8PhcmQ2m

# Hoặc nếu dùng Create React App:
REACT_APP_GOONG_MAP_KEY=Qu7Vly4VMWH8W5pYa8X1TCjzozBR21AY8PhcmQ2m
```

**Lưu ý:** 
- Backend dùng **API Key** (xlhtvme2wiJOTq93oXbPaO43ig0DbpSIwlQp3tWR)
- Frontend dùng **Map Key** (Qu7Vly4VMWH8W5pYa8X1TCjzozBR21AY8PhcmQ2m)

## 3. Component hiển thị bản đồ

### `ShopMap.tsx` - Hiển thị 1 shop trên map

```typescript
import { useEffect, useRef } from 'react';
import goongjs from '@goongmaps/goong-js';
import '@goongmaps/goong-js/dist/goong-js.css';

interface ShopMapProps {
  latitude: number;
  longitude: number;
  shopName: string;
}

export default function ShopMap({ latitude, longitude, shopName }: ShopMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<goongjs.Map | null>(null);

  useEffect(() => {
    if (!mapContainer.current) return;

    // Khởi tạo map
    goongjs.accessToken = import.meta.env.VITE_GOONG_MAP_KEY; // hoặc process.env.REACT_APP_GOONG_MAP_KEY
    
    map.current = new goongjs.Map({
      container: mapContainer.current,
      style: 'https://tiles.goong.io/assets/goong_map_web.json',
      center: [longitude, latitude],
      zoom: 15
    });

    // Thêm marker
    new goongjs.Marker({ color: '#FF0000' })
      .setLngLat([longitude, latitude])
      .setPopup(new goongjs.Popup().setHTML(`<h3>${shopName}</h3>`))
      .addTo(map.current);

    // Cleanup
    return () => {
      if (map.current) {
        map.current.remove();
      }
    };
  }, [latitude, longitude, shopName]);

  return <div ref={mapContainer} style={{ width: '100%', height: '400px' }} />;
}
```

### Sử dụng trong component:

```typescript
import ShopMap from './ShopMap';

function ShopDetail() {
  const [shop, setShop] = useState(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/shops/public/1')
      .then(res => res.json())
      .then(data => setShop(data.result));
  }, []);

  if (!shop) return <div>Loading...</div>;

  return (
    <div>
      <h1>{shop.shopName}</h1>
      <p>{shop.address}</p>
      
      {shop.latitude && shop.longitude && (
        <ShopMap 
          latitude={shop.latitude} 
          longitude={shop.longitude} 
          shopName={shop.shopName} 
        />
      )}
    </div>
  );
}
```

## 4. Tìm shop gần vị trí người dùng

### `NearbyShops.tsx`

```typescript
import { useState, useEffect } from 'react';
import axios from 'axios';

interface Shop {
  id: number;
  shopName: string;
  address: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  logoUrl: string;
  ratingAvg: number;
}

export default function NearbyShops() {
  const [shops, setShops] = useState<Shop[]>([]);
  const [loading, setLoading] = useState(false);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);

  // Lấy vị trí người dùng
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude
          });
        },
        (error) => {
          console.error('Error getting location:', error);
          // Fallback: Dùng vị trí mặc định (TP.HCM)
          setUserLocation({ lat: 10.8231, lng: 106.6297 });
        }
      );
    }
  }, []);

  // Tìm shop gần
  useEffect(() => {
    if (!userLocation) return;

    setLoading(true);
    axios.get(`http://localhost:8080/api/shops/nearby`, {
      params: {
        lat: userLocation.lat,
        lng: userLocation.lng,
        radius: 10 // Bán kính 10km
      }
    })
    .then(res => {
      setShops(res.data.result);
    })
    .catch(err => console.error(err))
    .finally(() => setLoading(false));
  }, [userLocation]);

  if (loading) return <div>Đang tìm shop gần bạn...</div>;

  return (
    <div>
      <h2>Shop gần bạn</h2>
      {shops.length === 0 ? (
        <p>Không tìm thấy shop nào trong bán kính 10km</p>
      ) : (
        <ul>
          {shops.map(shop => (
            <li key={shop.id}>
              <h3>{shop.shopName}</h3>
              <p>{shop.address}</p>
              <p>Cách bạn: {shop.distanceKm} km</p>
              <p>Đánh giá: {shop.ratingAvg} ⭐</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
```

## 5. Hiển thị nhiều shop trên map

### `ShopsMapView.tsx`

```typescript
import { useEffect, useRef, useState } from 'react';
import goongjs from '@goongmaps/goong-js';
import axios from 'axios';

export default function ShopsMapView() {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<goongjs.Map | null>(null);
  const [shops, setShops] = useState([]);

  useEffect(() => {
    // Lấy vị trí người dùng
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const userLat = position.coords.latitude;
        const userLng = position.coords.longitude;

        // Tìm shop gần
        axios.get('http://localhost:8080/api/shops/nearby', {
          params: { lat: userLat, lng: userLng, radius: 10 }
        }).then(res => {
          setShops(res.data.result);
          initMap(userLat, userLng, res.data.result);
        });
      },
      () => {
        // Fallback: TP.HCM
        const defaultLat = 10.8231;
        const defaultLng = 106.6297;
        axios.get('http://localhost:8080/api/shops/nearby', {
          params: { lat: defaultLat, lng: defaultLng, radius: 10 }
        }).then(res => {
          setShops(res.data.result);
          initMap(defaultLat, defaultLng, res.data.result);
        });
      }
    );
  }, []);

  const initMap = (userLat: number, userLng: number, shopList: any[]) => {
    if (!mapContainer.current) return;

    goongjs.accessToken = import.meta.env.VITE_GOONG_MAP_KEY;
    
    map.current = new goongjs.Map({
      container: mapContainer.current,
      style: 'https://tiles.goong.io/assets/goong_map_web.json',
      center: [userLng, userLat],
      zoom: 12
    });

    // Marker vị trí người dùng
    new goongjs.Marker({ color: '#0000FF' })
      .setLngLat([userLng, userLat])
      .setPopup(new goongjs.Popup().setHTML('<h3>Vị trí của bạn</h3>'))
      .addTo(map.current);

    // Marker các shop
    shopList.forEach(shop => {
      new goongjs.Marker({ color: '#FF0000' })
        .setLngLat([shop.longitude, shop.latitude])
        .setPopup(
          new goongjs.Popup().setHTML(`
            <div>
              <h3>${shop.shopName}</h3>
              <p>${shop.address}</p>
              <p>Cách bạn: ${shop.distanceKm} km</p>
              <a href="/shops/${shop.id}">Xem chi tiết</a>
            </div>
          `)
        )
        .addTo(map.current);
    });
  };

  return <div ref={mapContainer} style={{ width: '100%', height: '600px' }} />;
}
```

## 6. Chỉ đường đến shop

### `DirectionsMap.tsx`

```typescript
import { useEffect, useRef } from 'react';
import goongjs from '@goongmaps/goong-js';
import axios from 'axios';

interface DirectionsMapProps {
  shopId: number;
  shopLat: number;
  shopLng: number;
}

export default function DirectionsMap({ shopId, shopLat, shopLng }: DirectionsMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<goongjs.Map | null>(null);

  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const userLat = position.coords.latitude;
        const userLng = position.coords.longitude;

        // Gọi API backend để lấy directions
        axios.get(`http://localhost:8080/api/shops/${shopId}/directions`, {
          params: { fromLat: userLat, fromLng: userLng }
        }).then(res => {
          const directions = res.data.result;
          initMapWithDirections(userLat, userLng, shopLat, shopLng, directions);
        });
      }
    );
  }, [shopId, shopLat, shopLng]);

  const initMapWithDirections = (
    userLat: number, 
    userLng: number, 
    destLat: number, 
    destLng: number,
    directions: any
  ) => {
    if (!mapContainer.current) return;

    goongjs.accessToken = import.meta.env.VITE_GOONG_MAP_KEY;
    
    map.current = new goongjs.Map({
      container: mapContainer.current,
      style: 'https://tiles.goong.io/assets/goong_map_web.json',
      center: [(userLng + destLng) / 2, (userLat + destLat) / 2],
      zoom: 13
    });

    map.current.on('load', () => {
      // Vẽ đường đi
      if (directions.routes && directions.routes.length > 0) {
        const route = directions.routes[0];
        
        map.current!.addSource('route', {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {},
            geometry: route.overview_polyline.points // Goong trả về encoded polyline
          }
        });

        map.current!.addLayer({
          id: 'route',
          type: 'line',
          source: 'route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': '#3887be',
            'line-width': 5,
            'line-opacity': 0.75
          }
        });
      }

      // Marker điểm đầu và điểm cuối
      new goongjs.Marker({ color: '#0000FF' })
        .setLngLat([userLng, userLat])
        .setPopup(new goongjs.Popup().setHTML('<h3>Vị trí của bạn</h3>'))
        .addTo(map.current!);

      new goongjs.Marker({ color: '#FF0000' })
        .setLngLat([destLng, destLat])
        .setPopup(new goongjs.Popup().setHTML('<h3>Shop</h3>'))
        .addTo(map.current!);
    });
  };

  return <div ref={mapContainer} style={{ width: '100%', height: '500px' }} />;
}
```

## 7. API Endpoints từ Backend

### Tìm shop gần
```
GET http://localhost:8080/api/shops/nearby?lat=10.8231&lng=106.6297&radius=10
```

Response:
```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "shopName": "Pet Shop ABC",
      "address": "123 Nguyễn Văn Linh",
      "latitude": 10.8234,
      "longitude": 106.6298,
      "distanceKm": 0.5,
      "logoUrl": "...",
      "ratingAvg": 4.5
    }
  ]
}
```

### Lấy chỉ đường
```
GET http://localhost:8080/api/shops/1/directions?fromLat=10.8231&fromLng=106.6297
```

Response:
```json
{
  "code": 1000,
  "result": {
    "routes": [
      {
        "distance": 1234,
        "duration": 300,
        "overview_polyline": { "points": "..." }
      }
    ]
  }
}
```

### Lấy thông tin shop (có lat/lng)
```
GET http://localhost:8080/api/shops/public/1
```

Response:
```json
{
  "code": 1000,
  "result": {
    "id": 1,
    "shopName": "Pet Shop ABC",
    "address": "123 Nguyễn Văn Linh",
    "latitude": 10.8234,
    "longitude": 106.6298,
    ...
  }
}
```

## 8. Lưu ý quan trọng

1. **API Key vs Map Key:**
   - Backend dùng API Key: `xlhtvme2wiJOTq93oXbPaO43ig0DbpSIwlQp3tWR`
   - Frontend dùng Map Key: `Qu7Vly4VMWH8W5pYa8X1TCjzozBR21AY8PhcmQ2m`

2. **Thứ tự tọa độ:**
   - Goong Map: `[longitude, latitude]` (lng trước, lat sau)
   - Backend API: `?lat=...&lng=...` (lat trước, lng sau)

3. **Xin quyền location:**
   - Luôn có fallback khi user từ chối
   - Dùng HTTPS để `navigator.geolocation` hoạt động

4. **Performance:**
   - Chỉ khởi tạo map 1 lần trong useEffect
   - Cleanup map khi component unmount
   - Cache kết quả geocoding ở backend (đã có sẵn trong DB)

## 9. Tài liệu tham khảo

- Goong Map JS: https://docs.goong.io/
- Goong API: https://docs.goong.io/rest/
