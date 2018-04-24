package com.xebialabs.xlrelease.stress.domain

case class Comment(commentId: Comment.ID, author: String, date: String, text: String)

object Comment {
  type ID = String
}
