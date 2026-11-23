<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'send':
        $messageId   = $_POST['messageId'] ?? uniqid('m_');
        $senderUid   = $_POST['senderUid'] ?? '';
        $receiverUid = $_POST['receiverUid'] ?? '';
        $text        = $_POST['text'] ?? '';
        $timestamp   = $_POST['timestamp'] ?? (time() * 1000);

        $chatRoomId = ($senderUid < $receiverUid) ? "${senderUid}_${receiverUid}" : "${receiverUid}_${senderUid}";

        $stmt = $pdo->prepare("INSERT INTO chats (messageId, chatRoomId, senderUid, receiverUid, text, timestamp, status) VALUES (?, ?, ?, ?, ?, ?, 'sent')");
        $stmt->execute([$messageId, $chatRoomId, $senderUid, $receiverUid, $text, $timestamp]);

        // Add Notification
        $uStmt = $pdo->prepare("SELECT firstName, lastName FROM users WHERE uid = ?");
        $uStmt->execute([$senderUid]);
        $sender = $uStmt->fetch();
        $senderName = $sender ? trim($sender['firstName'] . ' ' . $sender['lastName']) : "Someone";

        $nStmt = $pdo->prepare("INSERT INTO notifications (notifId, receiverUid, senderUid, senderName, targetUid, type, message, timestamp) VALUES (?, ?, ?, ?, ?, 'chat', ?, ?)");
        $nStmt->execute([uniqid('n_'), $receiverUid, $senderUid, $senderName, $receiverUid, "sent you a message: \"$text\"", $timestamp]);

        echo json_encode(["status" => "success", "messageId" => $messageId]);
        break;

    case 'get':
        $chatRoomId = $_GET['chatRoomId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM chats WHERE chatRoomId = ? ORDER BY timestamp ASC");
        $stmt->execute([$chatRoomId]);
        $messages = $stmt->fetchAll();
        echo json_encode(["status" => "success", "messages" => $messages]);
        break;

    case 'edit':
        $messageId = $_POST['messageId'] ?? '';
        $text      = $_POST['text'] ?? '';
        $stmt = $pdo->prepare("UPDATE chats SET text = ?, isEdited = 1 WHERE messageId = ?");
        $stmt->execute([$text, $messageId]);
        echo json_encode(["status" => "success"]);
        break;

    case 'delete':
        $messageId = $_POST['messageId'] ?? '';
        $stmt = $pdo->prepare("DELETE FROM chats WHERE messageId = ?");
        $stmt->execute([$messageId]);
        echo json_encode(["status" => "success"]);
        break;

    case 'react':
        $messageId = $_POST['messageId'] ?? '';
        $reaction  = $_POST['reaction'] ?? '';
        $stmt = $pdo->prepare("UPDATE chats SET reaction = ? WHERE messageId = ?");
        $stmt->execute([$reaction, $messageId]);
        echo json_encode(["status" => "success"]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>