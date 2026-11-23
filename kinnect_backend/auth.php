<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'register':
        $uid       = $_POST['uid'] ?? uniqid('u_');
        $firstName = $_POST['firstName'] ?? '';
        $lastName  = $_POST['lastName'] ?? '';
        $birthday  = $_POST['birthday'] ?? '';
        $gender    = $_POST['gender'] ?? '';
        $email     = $_POST['email'] ?? '';
        $password  = $_POST['password'] ?? '';

        if (empty($email) || empty($password)) {
            echo json_encode(["status" => "error", "message" => "Email and password required"]);
            exit;
        }

        $stmt = $pdo->prepare("SELECT uid FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            echo json_encode(["status" => "error", "message" => "Email already registered"]);
            exit;
        }

        $sql = "INSERT INTO users (uid, firstName, lastName, birthday, gender, email, password, isOnline)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        $stmt = $pdo->prepare($sql);
        if ($stmt->execute([$uid, $firstName, $lastName, $birthday, $gender, $email, $password])) {
            echo json_encode(["status" => "success", "uid" => $uid, "message" => "User registered successfully"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Registration failed"]);
        }
        break;

    case 'login':
        $email    = $_POST['email'] ?? '';
        $password = $_POST['password'] ?? '';

        $stmt = $pdo->prepare("SELECT * FROM users WHERE email = ? AND password = ?");
        $stmt->execute([$email, $password]);
        $user = $stmt->fetch();

        if ($user) {
            $pdo->prepare("UPDATE users SET isOnline = 1 WHERE uid = ?")->execute([$user['uid']]);
            $user['isOnline'] = true;
            echo json_encode(["status" => "success", "user" => $user]);
        } else {
            echo json_encode(["status" => "error", "message" => "Invalid email or password"]);
        }
        break;

    case 'update_fcm_token':
        $uid      = $_POST['uid'] ?? '';
        $fcmToken = $_POST['fcmToken'] ?? '';
        if ($uid && $fcmToken) {
            $stmt = $pdo->prepare("UPDATE users SET fcmToken = ? WHERE uid = ?");
            $stmt->execute([$fcmToken, $uid]);
            echo json_encode(["status" => "success", "message" => "FCM Token updated"]);
        } else {
            echo json_encode(["status" => "error", "message" => "Missing parameters"]);
        }
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>