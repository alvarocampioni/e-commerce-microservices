package com.ms.user_service.service;

import org.springframework.stereotype.Service;

@Service
public class CodeGenerationService {

    private final static int size = 5;
    private final static char[] base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();

    public String generateCode(){
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < size; i++){
            code.append(base[(int)(Math.random()*base.length)]);
        }
        return code.toString();
    }
}
