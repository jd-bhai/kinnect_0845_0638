<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'create':
        $itemId      = $_POST['itemId'] ?? uniqid('m_');
        $sellerUid   = $_POST['sellerUid'] ?? '';
        $title       = $_POST['title'] ?? '';
        $price       = $_POST['price'] ?? '';
        $location    = $_POST['location'] ?? '';
        $description = $_POST['description'] ?? '';
        $image       = $_POST['image'] ?? '';
        $timestamp   = $_POST['timestamp'] ?? (time() * 1000);

        $stmt = $pdo->prepare("INSERT INTO marketplace (itemId, sellerUid, title, price, location, description, image, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$itemId, $sellerUid, $title, $price, $location, $description, $image, $timestamp]);
        echo json_encode(["status" => "success", "itemId" => $itemId]);
        break;

    case 'get_all':
        $stmt = $pdo->query("SELECT * FROM marketplace ORDER BY timestamp DESC");
        $items = $stmt->fetchAll();
        echo json_encode(["status" => "success", "items" => $items]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>