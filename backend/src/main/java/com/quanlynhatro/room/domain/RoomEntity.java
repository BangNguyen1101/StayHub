package com.quanlynhatro.room.domain;

import com.quanlynhatro.room.api.RoomStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Rooms", schema = "dbo")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomId", nullable = false)
    private Long id;

    @Column(name = "RoomCode", nullable = false, length = 50, unique = true)
    private String roomCode;

    @Column(name = "RoomName", length = 200)
    private String roomName;

    @Column(name = "FloorNumber", nullable = false)
    private Integer floorNumber;

    @Column(name = "AreaM2", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "BaseRent", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseRent;

    @Column(name = "MaxOccupants", nullable = false)
    private Integer maxOccupants;

    @Column(name = "AllowsSharing", nullable = false)
    private boolean allowsSharing;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "Notes", length = 1000)
    private String notes;

    @Generated(event = EventType.INSERT)
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Generated(event = EventType.INSERT)
    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    protected RoomEntity() {}

    public static RoomEntity create(
            String roomCode,
            String roomName,
            Integer floorNumber,
            BigDecimal areaM2,
            BigDecimal baseRent,
            Integer maxOccupants,
            Boolean allowsSharing,
            String notes) {
        RoomEntity room = new RoomEntity();
        room.roomCode = roomCode;
        room.roomName = roomName;
        room.floorNumber = floorNumber;
        room.areaM2 = areaM2;
        room.baseRent = baseRent;
        room.maxOccupants = maxOccupants;
        room.allowsSharing = allowsSharing;
        room.status = RoomStatus.VACANT.name();
        room.notes = notes;
        return room;
    }

    public void updateDetails(
            String roomCode,
            String roomName,
            Integer floorNumber,
            BigDecimal areaM2,
            BigDecimal baseRent,
            Integer maxOccupants,
            Boolean allowsSharing,
            String notes) {
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.floorNumber = floorNumber;
        this.areaM2 = areaM2;
        this.baseRent = baseRent;
        this.maxOccupants = maxOccupants;
        this.allowsSharing = allowsSharing;
        this.notes = notes;
    }

    public void changeStatus(RoomStatus status) {
        this.status = status.name();
    }

    public Long getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public BigDecimal getBaseRent() {
        return baseRent;
    }

    public Integer getMaxOccupants() {
        return maxOccupants;
    }

    public boolean isAllowsSharing() {
        return allowsSharing;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
