<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'create':
        $storyId   = $_POST['storyId'] ?? uniqid('s_');
        $userId    = $_POST['userId'] ?? '';
        $caption   = $_POST['caption'] ?? '';
        $mediaUri  = $_POST['mediaUri'] ?? '';
        $timestamp = $_POST['timestamp'] ?? (time() * 1000);

        $stmt = $pdo->prepare("INSERT INTO stories (storyId, userId, caption, mediaUri, timestamp) VALUES (?, ?, ?, ?, ?)");
        $stmt->execute([$storyId, $userId, $caption, $mediaUri, $timestamp]);
        echo json_encode(["status" => "success", "storyId" => $storyId]);
        break;

    case 'get_latest':
        $userId = $_GET['userId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM stories WHERE userId = ? ORDER BY timestamp DESC LIMIT 1");
        $stmt->execute([$userId]);
        $story = $stmt->fetch();
        echo json_encode(["status" => "success", "story" => $story ? $story : null]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>