<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'send_request':
        $senderUid   = $_POST['senderUid'] ?? '';
        $receiverUid = $_POST['receiverUid'] ?? '';

        $stmt = $pdo->prepare("REPLACE INTO friend_requests (receiverUid, senderUid) VALUES (?, ?)");
        $stmt->execute([$receiverUid, $senderUid]);

        $uStmt = $pdo->prepare("SELECT firstName, lastName FROM users WHERE uid = ?");
        $uStmt->execute([$senderUid]);
        $sender = $uStmt->fetch();
        $senderName = $sender ? trim($sender['firstName'] . ' ' . $sender['lastName']) : "Someone";

        $nStmt = $pdo->prepare("INSERT INTO notifications (notifId, receiverUid, senderUid, senderName, targetUid, type, message, timestamp) VALUES (?, ?, ?, ?, ?, 'friend_request', 'sent you a friend request.', ?)");
        $nStmt->execute([uniqid('n_'), $receiverUid, $senderUid, $senderName, $receiverUid, (time() * 1000)]);

        echo json_encode(["status" => "success"]);
        break;

    case 'accept_request':
        $currentUid = $_POST['currentUid'] ?? '';
        $senderUid  = $_POST['senderUid'] ?? '';

        $stmt1 = $pdo->prepare("REPLACE INTO friends (userUid, friendUid) VALUES (?, ?)");
        $stmt1->execute([$currentUid, $senderUid]);

        $stmt2 = $pdo->prepare("REPLACE INTO friends (userUid, friendUid) VALUES (?, ?)");
        $stmt2->execute([$senderUid, $currentUid]);

        $stmt3 = $pdo->prepare("DELETE FROM friend_requests WHERE receiverUid = ? AND senderUid = ?");
        $stmt3->execute([$currentUid, $senderUid]);

        echo json_encode(["status" => "success"]);
        break;

    case 'delete_request':
        $currentUid = $_POST['currentUid'] ?? '';
        $senderUid  = $_POST['senderUid'] ?? '';
        $stmt = $pdo->prepare("DELETE FROM friend_requests WHERE receiverUid = ? AND senderUid = ?");
        $stmt->execute([$currentUid, $senderUid]);
        echo json_encode(["status" => "success"]);
        break;

    case 'get_friends':
        $currentUid = $_GET['currentUid'] ?? '';
        $stmt = $pdo->prepare("SELECT u.* FROM users u INNER JOIN friends f ON u.uid = f.friendUid WHERE f.userUid = ?");
        $stmt->execute([$currentUid]);
        $friends = $stmt->fetchAll();
        echo json_encode(["status" => "success", "friends" => $friends]);
        break;

    case 'get_requests':
        $currentUid = $_GET['currentUid'] ?? '';
        $stmt = $pdo->prepare("SELECT u.* FROM users u INNER JOIN friend_requests fr ON u.uid = fr.senderUid WHERE fr.receiverUid = ?");
        $stmt->execute([$currentUid]);
        $requests = $stmt->fetchAll();
        echo json_encode(["status" => "success", "requests" => $requests]);
        break;

    case 'search':
        $query = $_GET['query'] ?? '';
        $currentUid = $_GET['currentUid'] ?? '';
        $pattern = "%$query%";
        $stmt = $pdo->prepare("SELECT * FROM users WHERE uid != ? AND (firstName LIKE ? OR lastName LIKE ? OR username LIKE ?)");
        $stmt->execute([$currentUid, $pattern, $pattern, $pattern]);
        $users = $stmt->fetchAll();
        echo json_encode(["status" => "success", "users" => $users]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>