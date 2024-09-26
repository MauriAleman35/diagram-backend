package com.DiagramParcialBackend.Response;

import com.DiagramParcialBackend.Entity.Session;
import lombok.Data;

@Data
public class SessionResponse {
    Session session;
    public SessionResponse(Session session){
        this.session=session;
    }
}
