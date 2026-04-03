-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: it9_data
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `role_id` int NOT NULL COMMENT '角色ID',
  `menu_id` int NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`),
  KEY `permission_id` (`menu_id`),
  CONSTRAINT `fk_role_menu_menu_id` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色与菜单的多对多关联表，用于控制角色可以访问的菜单项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (1,1),(2,1),(3,1),(1,2),(2,2),(1,3),(2,3),(3,3),(1,4),(2,4),(3,4),(4,5),(4,6),(4,7),(4,8),(4,9),(4,10),(4,11),(1,15),(2,15),(1,16),(2,16),(1,17),(2,17),(1,18),(2,18),(1,19),(2,19),(1,20),(2,20),(1,21),(2,21),(1,22),(2,22),(1,23),(2,23),(3,23),(1,24),(2,24),(1,25),(2,25),(3,25),(1,26),(2,26),(1,27),(2,27),(3,27),(1,28),(2,28),(3,28),(1,29),(2,29),(1,30),(2,30),(1,31),(2,31),(1,32),(2,32),(3,32),(1,33),(2,33),(3,33),(1,34),(2,34),(3,34),(1,35),(2,35),(1,36),(2,36),(1,37),(2,37),(1,38),(2,38),(3,38),(1,39),(2,39),(3,39),(1,40),(2,40),(1,41),(2,41),(1,42),(2,42),(3,42),(1,43),(2,43),(3,43),(1,44),(2,44),(3,44),(1,45),(2,45),(1,46),(2,46),(3,46),(1,47),(2,47),(3,47),(1,48),(2,48),(3,48),(1,49),(2,49),(3,49),(1,50),(2,50),(1,51),(2,51),(1,52),(2,52),(1,53),(2,53),(1,54),(2,54),(1,55),(2,55),(1,56),(2,56),(1,57),(2,57),(3,57),(1,58),(2,58),(3,58),(1,59),(2,59),(1,60),(2,60),(1,61),(2,61),(1,62),(2,62),(1,63),(2,63),(1,64),(2,64),(1,65),(2,65),(1,66),(2,66),(1,67),(2,67),(1,68),(2,68),(1,69),(2,69),(1,70),(2,70),(1,71),(2,71),(1,72),(2,72),(1,73),(2,73),(1,74),(2,74),(1,75),(2,75),(1,76),(2,76),(1,77),(2,77),(1,78),(2,78),(1,79),(2,79),(1,80),(2,80),(1,81),(2,81),(1,82),(2,82),(1,83),(2,83),(1,84),(2,84),(1,85),(2,85),(1,86),(1,87),(2,87),(1,88),(2,88),(1,89),(1,90),(1,91),(1,92),(1,93),(2,93),(1,94),(2,94),(1,95),(1,96),(1,97),(1,98),(1,99),(1,100),(1,101),(2,101),(1,102),(1,103),(1,104),(2,104),(1,105),(2,105),(1,106),(2,106),(1,107),(2,107),(1,108),(2,108),(1,109),(2,109),(1,110),(2,110),(1,111),(2,111),(1,112),(2,112),(1,113),(2,113),(1,114),(2,114),(1,115),(2,115),(1,116),(2,116),(1,117),(2,117),(1,118),(2,118),(3,118),(1,119),(2,119),(3,119),(1,120),(2,120),(4,121),(4,122),(4,123),(4,124);
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03 23:03:44
