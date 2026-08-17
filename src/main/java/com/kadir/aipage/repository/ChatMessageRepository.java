package com.kadir.aipage.repository;


import com.kadir.aipage.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public  interface ChatMessageRepository extends JpaRepository <ChatMessage, Long> {

}
