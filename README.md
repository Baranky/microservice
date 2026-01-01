# Spring Cloud Microservices Demo: Event-Driven Architecture with Saga Pattern

Bu proje, **Spring Boot 3** ve **Spring Cloud** teknolojileri kullanılarak geliştirilmiş, ölçeklenebilir ve hataya dayanıklı (fault-tolerant) bir mikroservis mimarisi örneğidir. Proje temel olarak Service Discovery, API Gateway, **Resilience4j ile Circuit Breaker**, **Event-Driven Architecture (Kafka)**, **Saga Pattern (Choreography)**, ve **Distributed Tracing (Zipkin)** desenlerini demonstrasyonunu içerir.

##  İçindekiler

- [Mimari ve Servisler]
- [Saga Pattern İş Akışı]
- [Teknolojiler]
- [Kurulum ve Çalıştırma]
- [API Endpoints]
- [Monitoring ve Tracing]

##  Mimari ve Servisler

Proje 6 ana mikroservisten oluşur:

| Servis | Port | Açıklama |
| :--- |:-------| :--- |
| **EurekaService** | `8761` | Service Discovery sunucusu. Tüm servislerin kayıt defteridir. |
| **ApiGateway** | `8082` | Dış dünyaya açılan tek kapı (Entry Point). Spring Cloud Gateway. |
| **ProductService** | `8092` | Ürün yönetim servisi. PostgreSQL veritabanı kullanır. Ürün oluşturulduğunda InventoryService'e stock bilgisini gönderir. |
| **OrderService** | `8093` | Sipariş servisi. Resilience4j Circuit Breaker içerir. Sipariş oluşturulduğunda Kafka'ya event gönderir. |
| **InventoryService** | `8094` | Stok yönetim servisi. PostgreSQL veritabanı kullanır. Kafka'dan sipariş event'lerini dinler ve stok düşürür. |
| **PaymentService** | `8095` | Ödeme servisi. PostgreSQL veritabanı kullanır. Saga Pattern ile ödeme işlemlerini yönetir. |

##  Saga Pattern İş Akışı

### Başarılı Sipariş Akışı

1. **Sipariş Oluşturma:**
   - Kullanıcı `OrderService`'e sipariş oluşturur
   - `OrderService` siparişi veritabanına kaydeder (status: `PENDING`)
   - `OrderService` Kafka'ya `order-placed` event gönderir

2. **Stok Rezervasyonu:**
   - `InventoryService` `order-placed` event'ini dinler
   - Stok kontrolü yapar
   - Stok yeterliyse stok düşülür ve `stock-reserved` event yayınlanır
   - Stok yetersizse `order-cancelled` event yayınlanır

3. **Ödeme İşlemi:**
   - `PaymentService` `stock-reserved` event'ini dinler
   - PENDING status ile Payment kaydı oluşturur
   - Manuel ödeme için API endpoint'i bekler

4. **Manuel Ödeme:**
   - `POST /api/payments/{id}/process` endpoint'i çağrılır
   - Ödeme başarılı olursa `payment-confirmed` event yayınlanır
   - Ödeme reddedilirse (`POST /api/payments/{id}/reject`) `payment-failed` event yayınlanır

5. **Sipariş Onaylama:**
   - `OrderService` `payment-confirmed` event'ini dinler
   - Sipariş status `CONFIRMED` olur

### Compensation (Geri Alma) Akışı

Ödeme başarısız olduğunda:

1. `PaymentService` → `payment-failed` event yayınlar
2. `InventoryService` → Stokları geri ekler ve `stock-released` event yayınlar
3. `OrderService` → Sipariş status `CANCELLED` olur

## Teknolojiler

- **Dil:** Java 21
- **Framework:** Spring Boot 3.5.x, Spring Cloud 2025.0.0
- **Discovery:** Netflix Eureka Client / Server
- **Gateway:** Spring Cloud Gateway
- **İletişim:** 
  - Spring Cloud OpenFeign (Senkron servis çağrıları)
  - Apache Kafka (Asenkron event-driven iletişim)
- **Pattern:** Saga Pattern (Choreography)
- **Resilience:** Spring Cloud Circuit Breaker (Resilience4j)
- **Tracing:** Zipkin (Distributed Tracing)
- **Veritabanı:** PostgreSQL, Spring Data JPA
- **Mesajlaşma:** Apache Kafka, Zookeeper
- **Araçlar:** Lombok, Maven, Docker Compose

## Docker Compose Servisleri

Proje kök dizinindeki `docker-compose.yml` dosyası şu servisleri içerir:

| Servis | Port | Açıklama |
| :--- | :--- | :--- |
| **Zookeeper** | `2181` | Kafka için koordinasyon servisi |
| **Kafka** | `9092` | Event streaming platformu |
| **Kafka UI** | `8096` | Kafka topic'lerini görüntülemek için web arayüzü |
| **Zipkin** | `9411` | Distributed tracing UI |

## Kurulum ve Çalıştırma

### 1. Docker Servislerini Başlat

```bash
docker compose up -d
```

Bu komut şunları başlatır:
- Zookeeper
- Kafka
- Kafka UI (http://localhost:8096)
- Zipkin (http://localhost:9411)

### 2. PostgreSQL Veritabanlarını Oluştur

Aşağıdaki veritabanlarını PostgreSQL'de oluşturun:
- `product` (ProductService için)
- `orderdb` (OrderService için)
- `inventory` (InventoryService için)
- `paymentt` (PaymentService için)

### 3. Servisleri Başlat (Sırayla)

1. **EurekaService** - Service Discovery (Port: 8761)
2. **ProductService** - Port 8092
3. **OrderService** - Port 8093
4. **InventoryService** - Port 8094
5. **PaymentService** - Port 8095
6. **ApiGateway** - Port 8082

##  API Endpoints

### ProductService (Port: 8092)

- `GET /api/products` - Tüm ürünleri listele
- `GET /api/products/{id}` - Ürün detayı
- `POST /api/products` - Yeni ürün oluştur (stock bilgisi ile)
- `PUT /api/products/{id}` - Ürün güncelle
- `DELETE /api/products/{id}` - Ürün sil (InventoryService'den de silinir)

### OrderService (Port: 8093)

- `GET /api/orders` - Tüm siparişleri listele
- `GET /api/orders/{id}` - Sipariş detayı
- `GET /api/orders/customer/{email}` - Müşteri siparişleri
- `POST /api/orders` - Yeni sipariş oluştur (Kafka'ya event gönderir)
- `PUT /api/orders/{id}` - Sipariş güncelle
- `DELETE /api/orders/{id}` - Sipariş sil

### InventoryService (Port: 8094)

- `POST /api/inventory` - Stok kaydı oluştur
- `DELETE /api/inventory/product/{productId}` - Ürün stok kaydını sil

### PaymentService (Port: 8095)

- `GET /api/payments` - Tüm ödemeleri listele
- `GET /api/payments/{id}` - Ödeme detayı
- `GET /api/payments/order/{orderId}` - Order ID'ye göre ödeme getir
- `GET /api/payments/pending` - PENDING status'lu ödemeleri listele
- `GET /api/payments/failed` - Başarısız ödemeleri listele (retry için)
- `POST /api/payments/{id}/process` - Manuel ödeme işlemini gerçekleştir
- `POST /api/payments/{id}/reject` - Ödemeyi reddet (sipariş iptal edilir)
- `POST /api/payments/{id}/retry` - Ödeme işlemini tekrar dene

##  Monitoring ve Tracing

### Eureka Dashboard

- **URL:** http://localhost:8761
- Servislerin kayıtlı olup olmadığını kontrol etmek için

### Kafka UI

- **URL:** http://localhost:8096
- Topic'leri ve event'leri görüntülemek için

### Zipkin UI

- **URL:** http://localhost:9411
- Distributed tracing için tüm servisler arasındaki istek akışını görüntüleyebilirsiniz

##  Kafka Topics

| Topic | Açıklama | Publisher | Consumer |
| :--- | :--- | :--- | :--- |
| `order-placed` | Sipariş oluşturulduğunda | OrderService | InventoryService |
| `stock-reserved` | Stok rezerve edildiğinde | InventoryService | PaymentService |
| `stock-released` | Stok geri bırakıldığında | InventoryService | OrderService |
| `payment-confirmed` | Ödeme başarılı olduğunda | PaymentService | OrderService |
| `payment-failed` | Ödeme başarısız olduğunda | PaymentService | InventoryService |
| `order-cancelled` | Sipariş iptal edildiğinde | InventoryService | OrderService |

## Test Araçları

- **Postman / cURL** - API istekleri için
- **Kafka UI** - Event'leri görüntülemek için
- **Zipkin** - Transaction trace'lerini görüntülemek için
- **Eureka Dashboard** - Servis durumunu kontrol etmek için

##  Önemli Notlar

- Tüm servisler Eureka'ya kayıtlı olmalıdır
- Kafka ve Zookeeper çalışıyor olmalıdır
- PostgreSQL veritabanları oluşturulmuş olmalıdır
- Ödeme işlemi manuel olarak API üzerinden yapılır
- Saga Pattern Choreography yaklaşımı kullanılmaktadır

