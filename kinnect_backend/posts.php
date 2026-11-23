<?php
require_once 'db.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

switch ($action) {
    case 'create':
        $postId    = $_POST['postId'] ?? uniqid('p_');
        $authorUid = $_POST['authorUid'] ?? '';
        $caption   = $_POST['caption'] ?? '';
        $imageUri  = $_POST['imageUri'] ?? '';
        $timestamp = $_POST['timestamp'] ?? (time() * 1000);

        $stmt = $pdo->prepare("INSERT INTO posts (postId, authorUid, caption, imageUri, timestamp) VALUES (?, ?, ?, ?, ?)");
        if ($stmt->execute([$postId, $authorUid, $caption, $imageUri, $timestamp])) {
            echo json_encode(["status" => "success", "postId" => $postId]);
        } else {
            echo json_encode(["status" => "error", "message" => "Failed to create post"]);
        }
        break;

    case 'get_all':
        $currentUid = $_GET['currentUid'] ?? '';
        $stmt = $pdo->query("SELECT p.*, u.firstName, u.lastName, u.profilePicUrl FROM posts p LEFT JOIN users u ON p.authorUid = u.uid ORDER BY p.timestamp DESC");
        $posts = $stmt->fetchAll();

        foreach ($posts as &$post) {
            $rStmt = $pdo->prepare("SELECT COUNT(*) as cnt FROM post_reactions WHERE postId = ?");
            $rStmt->execute([$post['postId']]);
            $post['reactionsCount'] = (int)$rStmt->fetch()['cnt'];

            $userReactStmt = $pdo->prepare("SELECT type FROM post_reactions WHERE postId = ? AND userUid = ?");
            $userReactStmt->execute([$post['postId'], $currentUid]);
            $userReact = $userReactStmt->fetch();
            $post['userReaction'] = $userReact ? $userReact['type'] : '';
        }

        echo json_encode(["status" => "success", "posts" => $posts]);
        break;

    case 'react':
        $postId  = $_POST['postId'] ?? '';
        $userUid = $_POST['userUid'] ?? '';
        $type    = $_POST['type'] ?? '';

        if (empty($type)) {
            $stmt = $pdo->prepare("DELETE FROM post_reactions WHERE postId = ? AND userUid = ?");
            $stmt->execute([$postId, $userUid]);
        } else {
            $stmt = $pdo->prepare("REPLACE INTO post_reactions (postId, userUid, type) VALUES (?, ?, ?)");
            $stmt->execute([$postId, $userUid, $type]);
        }
        echo json_encode(["status" => "success"]);
        break;

    case 'add_comment':
        $commentId       = $_POST['commentId'] ?? uniqid('c_');
        $postId          = $_POST['postId'] ?? '';
        $parentCommentId = $_POST['parentCommentId'] ?? '';
        $authorUid       = $_POST['authorUid'] ?? '';
        $authorName      = $_POST['authorName'] ?? '';
        $text            = $_POST['text'] ?? '';
        $timestamp       = $_POST['timestamp'] ?? (time() * 1000);

        $stmt = $pdo->prepare("INSERT INTO post_comments (commentId, postId, parentCommentId, authorUid, authorName, text, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([$commentId, $postId, $parentCommentId, $authorUid, $authorName, $text, $timestamp]);
        echo json_encode(["status" => "success", "commentId" => $commentId]);
        break;

    case 'get_comments':
        $postId = $_GET['postId'] ?? '';
        $stmt = $pdo->prepare("SELECT * FROM post_comments WHERE postId = ? ORDER BY timestamp ASC");
        $stmt->execute([$postId]);
        $comments = $stmt->fetchAll();
        echo json_encode(["status" => "success", "comments" => $comments]);
        break;

    default:
        echo json_encode(["status" => "error", "message" => "Invalid action"]);
        break;
}
?>