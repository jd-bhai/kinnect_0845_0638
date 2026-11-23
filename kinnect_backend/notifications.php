<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'get':
        $currentUid = $_GET['currentUid'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM notifications WHERE receiverUid = ? ORDER BY timestamp DESC");
        $stmt->execute([$currentUid]);
        $notifications = $stmt->fetchAll();
        echo json_encode(["status" => "success", "notifications" => $notifications]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>