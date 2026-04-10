CREATE DATABASE  IF NOT EXISTS `IPOS-CA` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `IPOS-CA`;
-- MySQL dump 10.13  Distrib 8.0.36, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: IPOS-CA
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `product_id` int NOT NULL AUTO_INCREMENT,
  `item_id` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `package_type` varchar(100) DEFAULT NULL,
  `units_in_pack` int NOT NULL DEFAULT '1',
  `price` decimal(10,2) NOT NULL,
  `vat_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
  `stock_quantity` int NOT NULL DEFAULT '0',
  `min_stock_level` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `item_id` (`item_id`),
  CONSTRAINT `products_chk_1` CHECK ((`units_in_pack` > 0)),
  CONSTRAINT `products_chk_2` CHECK ((`price` >= 0)),
  CONSTRAINT `products_chk_3` CHECK ((`vat_rate` >= 0)),
  CONSTRAINT `products_chk_4` CHECK ((`stock_quantity` >= 0)),
  CONSTRAINT `products_chk_5` CHECK ((`min_stock_level` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'1','asas','adada',1,2.00,0.00,195,5,1),(2,'100 00001','Paracetamol 500mg Tablets','box',20,2.50,20.00,800,50,1),(3,'100 00002','Ibuprofen 200mg Tablets','box',24,3.20,20.00,800,40,1),(4,'100 00003','Aspirin 300mg Tablets','box',30,1.80,20.00,600,30,1),(5,'100 00004','Cough Syrup 100ml','bottle',1,4.50,20.00,200,20,1),(6,'100 00005','Vitamin C 500mg','box',60,5.99,20.00,500,25,1),(7,'200 00001','Amoxicillin 250mg Capsules','box',21,8.50,0.00,300,15,1),(8,'200 00002','Omeprazole 20mg Capsules','box',28,6.75,20.00,400,20,1),(9,'200 00003','Metformin 500mg Tablets','box',56,4.25,20.00,350,18,1),(10,'300 00001','Antiseptic Cream 30g','tube',1,3.99,20.00,250,15,1),(11,'300 00002','Bandages Pack ( assorted )','box',1,2.75,20.00,400,25,1),(12,'300 00003','Medical Gloves (100 pack)','box',100,8.99,20.00,600,30,1),(13,'400 00001','Blood Pressure Monitor','unit',1,45.00,20.00,50,5,1),(14,'400 00002','Digital Thermometer','unit',1,12.50,20.00,100,10,1),(15,'400 00003','First Aid Kit ( Premium )','kit',1,25.00,20.00,75,8,1);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-10  0:56:30
