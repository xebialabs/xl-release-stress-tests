package com.xebialabs.xlrelease.stress.domain

case class Comment(commentId: Comment.ID, comment: String)

object Comment {
  case class ID(taskId: Task.ID, commentId: String)
}
