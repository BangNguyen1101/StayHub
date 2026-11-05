/*
    QuanLyNhaTro - SQL Server database schema
    Generated from module_describe.md.

    Safety:
    - Creates the database only when it does not exist.
    - Does not DROP, TRUNCATE, or delete existing data.
    - Run this script with an account allowed to create a database.
    - Application secrets (JWT secret, SMTP password, payment secrets) are not stored here.
*/

USE master;
GO

IF DB_ID(N'QuanLyNhaTro') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [QuanLyNhaTro]');
END;
GO

USE [QuanLyNhaTro];
GO

IF OBJECT_ID(N'dbo.SchemaVersions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SchemaVersions
    (
        VersionNumber INT NOT NULL CONSTRAINT PK_SchemaVersions PRIMARY KEY,
        Description NVARCHAR(500) NOT NULL,
        AppliedAt DATETIME2(3) NOT NULL CONSTRAINT DF_SchemaVersions_AppliedAt DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF OBJECT_ID(N'dbo.Roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles
    (
        RoleId TINYINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Roles PRIMARY KEY,
        RoleCode VARCHAR(20) NOT NULL CONSTRAINT UQ_Roles_RoleCode UNIQUE,
        RoleName NVARCHAR(100) NOT NULL,
        CONSTRAINT CK_Roles_RoleCode CHECK (RoleCode IN ('ADMIN', 'TENANT'))
    );
END;
GO

IF OBJECT_ID(N'dbo.AppUsers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppUsers
    (
        UserId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AppUsers PRIMARY KEY,
        Username NVARCHAR(100) NOT NULL CONSTRAINT UQ_AppUsers_Username UNIQUE,
        Email NVARCHAR(320) NOT NULL CONSTRAINT UQ_AppUsers_Email UNIQUE,
        PasswordHash NVARCHAR(500) NOT NULL,
        RoleId TINYINT NOT NULL,
        FullName NVARCHAR(200) NOT NULL,
        PhoneNumber NVARCHAR(30) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_AppUsers_IsActive DEFAULT 1,
        LastLoginAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_AppUsers_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_AppUsers_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_AppUsers_Roles FOREIGN KEY (RoleId) REFERENCES dbo.Roles(RoleId)
    );
END;
GO

IF OBJECT_ID(N'dbo.AuthSessions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AuthSessions
    (
        SessionId UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_AuthSessions PRIMARY KEY DEFAULT NEWSEQUENTIALID(),
        UserId BIGINT NOT NULL,
        TokenHash NVARCHAR(500) NOT NULL CONSTRAINT UQ_AuthSessions_TokenHash UNIQUE,
        ExpiresAt DATETIME2(3) NOT NULL,
        RevokedAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_AuthSessions_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_AuthSessions_AppUsers FOREIGN KEY (UserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.PasswordResetTokens', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PasswordResetTokens
    (
        PasswordResetTokenId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PasswordResetTokens PRIMARY KEY,
        UserId BIGINT NOT NULL,
        TokenHash NVARCHAR(500) NOT NULL CONSTRAINT UQ_PasswordResetTokens_TokenHash UNIQUE,
        ExpiresAt DATETIME2(3) NOT NULL,
        UsedAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_PasswordResetTokens_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_PasswordResetTokens_AppUsers FOREIGN KEY (UserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.SystemConfigs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemConfigs
    (
        ConfigKey VARCHAR(100) NOT NULL CONSTRAINT PK_SystemConfigs PRIMARY KEY,
        ConfigValue NVARCHAR(1000) NOT NULL,
        Description NVARCHAR(500) NULL,
        IsSecret BIT NOT NULL CONSTRAINT DF_SystemConfigs_IsSecret DEFAULT 0,
        UpdatedByUserId BIGINT NULL,
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_SystemConfigs_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_SystemConfigs_UpdatedBy FOREIGN KEY (UpdatedByUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.Rooms', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Rooms
    (
        RoomId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Rooms PRIMARY KEY,
        RoomCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_Rooms_RoomCode UNIQUE,
        RoomName NVARCHAR(200) NULL,
        FloorNumber INT NOT NULL,
        AreaM2 DECIMAL(10,2) NOT NULL,
        BaseRent DECIMAL(19,2) NOT NULL,
        MaxOccupants INT NOT NULL,
        AllowsSharing BIT NOT NULL CONSTRAINT DF_Rooms_AllowsSharing DEFAULT 0,
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Rooms_Status DEFAULT 'VACANT',
        Notes NVARCHAR(1000) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Rooms_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Rooms_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_Rooms_AreaM2 CHECK (AreaM2 > 0),
        CONSTRAINT CK_Rooms_BaseRent CHECK (BaseRent >= 0),
        CONSTRAINT CK_Rooms_MaxOccupants CHECK (MaxOccupants >= 1),
        CONSTRAINT CK_Rooms_Status CHECK (Status IN ('VACANT', 'OCCUPIED', 'FULL', 'MAINTENANCE', 'INACTIVE'))
    );
END;
GO

IF OBJECT_ID(N'dbo.TenantProfiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TenantProfiles
    (
        TenantProfileId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TenantProfiles PRIMARY KEY,
        UserId BIGINT NOT NULL CONSTRAINT UQ_TenantProfiles_UserId UNIQUE,
        ContactAddress NVARCHAR(500) NULL,
        IdentityDocumentType NVARCHAR(50) NULL,
        IdentityDocumentNumber NVARCHAR(100) NULL,
        IdentityIssuedDate DATE NULL,
        IdentityIssuedPlace NVARCHAR(200) NULL,
        Notes NVARCHAR(1000) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_TenantProfiles_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_TenantProfiles_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_TenantProfiles_AppUsers FOREIGN KEY (UserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.RentalRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RentalRequests
    (
        RentalRequestId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_RentalRequests PRIMARY KEY,
        RequestCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_RentalRequests_RequestCode UNIQUE,
        ApplicantUserId BIGINT NOT NULL,
        RoomId BIGINT NOT NULL,
        ContactName NVARCHAR(200) NOT NULL,
        ContactPhone NVARCHAR(30) NOT NULL,
        ContactEmail NVARCHAR(320) NULL,
        ExpectedOccupantCount INT NOT NULL,
        ExpectedMoveInDate DATE NOT NULL,
        Note NVARCHAR(2000) NULL,
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_RentalRequests_Status DEFAULT 'PENDING',
        ProcessedByUserId BIGINT NULL,
        ProcessedAt DATETIME2(3) NULL,
        ProcessingNote NVARCHAR(2000) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_RentalRequests_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_RentalRequests_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_RentalRequests_Applicant FOREIGN KEY (ApplicantUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_RentalRequests_Room FOREIGN KEY (RoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_RentalRequests_ProcessedBy FOREIGN KEY (ProcessedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_RentalRequests_OccupantCount CHECK (ExpectedOccupantCount >= 1),
        CONSTRAINT CK_RentalRequests_Status CHECK (Status IN ('PENDING', 'NEEDS_INFO', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.RentalRequestHistories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RentalRequestHistories
    (
        RentalRequestHistoryId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_RentalRequestHistories PRIMARY KEY,
        RentalRequestId BIGINT NOT NULL,
        OldStatus VARCHAR(20) NULL,
        NewStatus VARCHAR(20) NOT NULL,
        Note NVARCHAR(2000) NULL,
        ChangedByUserId BIGINT NOT NULL,
        ChangedAt DATETIME2(3) NOT NULL CONSTRAINT DF_RentalRequestHistories_ChangedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_RentalRequestHistories_Request FOREIGN KEY (RentalRequestId) REFERENCES dbo.RentalRequests(RentalRequestId),
        CONSTRAINT FK_RentalRequestHistories_User FOREIGN KEY (ChangedByUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.Contracts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Contracts
    (
        ContractId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Contracts PRIMARY KEY,
        ContractCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_Contracts_ContractCode UNIQUE,
        TenantUserId BIGINT NOT NULL,
        RoomId BIGINT NOT NULL,
        RentalRequestId BIGINT NULL,
        StartDate DATE NOT NULL,
        EndDate DATE NOT NULL,
        ActualEndDate DATE NULL,
        MonthlyRent DECIMAL(19,2) NOT NULL,
        DepositAmount DECIMAL(19,2) NOT NULL,
        BillingCycleMonths INT NOT NULL CONSTRAINT DF_Contracts_BillingCycleMonths DEFAULT 1,
        DueDay TINYINT NOT NULL CONSTRAINT DF_Contracts_DueDay DEFAULT 5,
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Contracts_Status DEFAULT 'DRAFT',
        Terms NVARCHAR(MAX) NULL,
        Notes NVARCHAR(2000) NULL,
        CreatedByUserId BIGINT NOT NULL,
        ActivatedAt DATETIME2(3) NULL,
        TerminatedAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Contracts_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Contracts_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Contracts_Tenant FOREIGN KEY (TenantUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_Contracts_Room FOREIGN KEY (RoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_Contracts_RentalRequest FOREIGN KEY (RentalRequestId) REFERENCES dbo.RentalRequests(RentalRequestId),
        CONSTRAINT FK_Contracts_CreatedBy FOREIGN KEY (CreatedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_Contracts_DateRange CHECK (EndDate >= StartDate),
        CONSTRAINT CK_Contracts_ActualEndDate CHECK (ActualEndDate IS NULL OR ActualEndDate >= StartDate),
        CONSTRAINT CK_Contracts_MonthlyRent CHECK (MonthlyRent >= 0),
        CONSTRAINT CK_Contracts_Deposit CHECK (DepositAmount >= 0),
        CONSTRAINT CK_Contracts_BillingCycle CHECK (BillingCycleMonths >= 1),
        CONSTRAINT CK_Contracts_DueDay CHECK (DueDay BETWEEN 1 AND 28),
        CONSTRAINT CK_Contracts_Status CHECK (Status IN ('DRAFT', 'PENDING', 'ACTIVE', 'EXPIRING', 'EXPIRED', 'TERMINATED', 'CANCELLED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.ContractOccupants', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ContractOccupants
    (
        ContractOccupantId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ContractOccupants PRIMARY KEY,
        ContractId BIGINT NOT NULL,
        UserId BIGINT NULL,
        FullName NVARCHAR(200) NOT NULL,
        PhoneNumber NVARCHAR(30) NULL,
        IdentityDocumentNumber NVARCHAR(100) NULL,
        IsPrimaryTenant BIT NOT NULL CONSTRAINT DF_ContractOccupants_IsPrimaryTenant DEFAULT 0,
        MoveInDate DATE NOT NULL,
        MoveOutDate DATE NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_ContractOccupants_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_ContractOccupants_Contract FOREIGN KEY (ContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_ContractOccupants_User FOREIGN KEY (UserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_ContractOccupants_DateRange CHECK (MoveOutDate IS NULL OR MoveOutDate >= MoveInDate)
    );
END;
GO

IF OBJECT_ID(N'dbo.ContractHistories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ContractHistories
    (
        ContractHistoryId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ContractHistories PRIMARY KEY,
        ContractId BIGINT NOT NULL,
        ActionCode VARCHAR(30) NOT NULL,
        BeforeData NVARCHAR(MAX) NULL,
        AfterData NVARCHAR(MAX) NULL,
        PerformedByUserId BIGINT NOT NULL,
        PerformedAt DATETIME2(3) NOT NULL CONSTRAINT DF_ContractHistories_PerformedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_ContractHistories_Contract FOREIGN KEY (ContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_ContractHistories_User FOREIGN KEY (PerformedByUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.RoomTransfers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RoomTransfers
    (
        RoomTransferId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_RoomTransfers PRIMARY KEY,
        TenantUserId BIGINT NOT NULL,
        OldRoomId BIGINT NOT NULL,
        NewRoomId BIGINT NOT NULL,
        OldContractId BIGINT NULL,
        NewContractId BIGINT NULL,
        TransferDate DATE NOT NULL,
        OldRent DECIMAL(19,2) NULL,
        NewRent DECIMAL(19,2) NULL,
        DepositHandling NVARCHAR(1000) NULL,
        DebtHandling NVARCHAR(1000) NULL,
        Note NVARCHAR(2000) NULL,
        PerformedByUserId BIGINT NOT NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_RoomTransfers_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_RoomTransfers_Tenant FOREIGN KEY (TenantUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_RoomTransfers_OldRoom FOREIGN KEY (OldRoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_RoomTransfers_NewRoom FOREIGN KEY (NewRoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_RoomTransfers_OldContract FOREIGN KEY (OldContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_RoomTransfers_NewContract FOREIGN KEY (NewContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_RoomTransfers_PerformedBy FOREIGN KEY (PerformedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_RoomTransfers_DifferentRooms CHECK (OldRoomId <> NewRoomId)
    );
END;
GO

IF OBJECT_ID(N'dbo.FeeConfigs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FeeConfigs
    (
        FeeConfigId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_FeeConfigs PRIMARY KEY,
        FeeCode VARCHAR(30) NOT NULL,
        FeeName NVARCHAR(200) NOT NULL,
        UnitName NVARCHAR(50) NOT NULL,
        UnitPrice DECIMAL(19,2) NOT NULL,
        PricingMode VARCHAR(20) NOT NULL,
        EffectiveFrom DATE NOT NULL,
        EffectiveTo DATE NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_FeeConfigs_IsActive DEFAULT 1,
        CreatedByUserId BIGINT NOT NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_FeeConfigs_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_FeeConfigs_CreatedBy FOREIGN KEY (CreatedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_FeeConfigs_UnitPrice CHECK (UnitPrice >= 0),
        CONSTRAINT CK_FeeConfigs_PricingMode CHECK (PricingMode IN ('PER_ROOM', 'PER_PERSON', 'PER_USAGE')),
        CONSTRAINT CK_FeeConfigs_DateRange CHECK (EffectiveTo IS NULL OR EffectiveTo >= EffectiveFrom)
    );
END;
GO

IF OBJECT_ID(N'dbo.Services', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Services
    (
        ServiceId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Services PRIMARY KEY,
        ServiceCode VARCHAR(30) NOT NULL CONSTRAINT UQ_Services_ServiceCode UNIQUE,
        ServiceName NVARCHAR(200) NOT NULL,
        PricingMode VARCHAR(20) NOT NULL,
        DefaultUnitPrice DECIMAL(19,2) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_Services_IsActive DEFAULT 1,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Services_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT CK_Services_PricingMode CHECK (PricingMode IN ('PER_ROOM', 'PER_PERSON', 'PER_USAGE')),
        CONSTRAINT CK_Services_DefaultUnitPrice CHECK (DefaultUnitPrice >= 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.UtilityReadings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.UtilityReadings
    (
        UtilityReadingId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_UtilityReadings PRIMARY KEY,
        RoomId BIGINT NOT NULL,
        UtilityType VARCHAR(20) NOT NULL,
        BillingPeriod CHAR(7) NOT NULL,
        ReadingStart DECIMAL(18,3) NOT NULL,
        ReadingEnd DECIMAL(18,3) NOT NULL,
        ReadingDate DATE NOT NULL,
        IsLocked BIT NOT NULL CONSTRAINT DF_UtilityReadings_IsLocked DEFAULT 0,
        Note NVARCHAR(1000) NULL,
        RecordedByUserId BIGINT NOT NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_UtilityReadings_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_UtilityReadings_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_UtilityReadings_RoomTypePeriod UNIQUE (RoomId, UtilityType, BillingPeriod),
        CONSTRAINT FK_UtilityReadings_Room FOREIGN KEY (RoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_UtilityReadings_RecordedBy FOREIGN KEY (RecordedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_UtilityReadings_Type CHECK (UtilityType IN ('ELECTRICITY', 'WATER')),
        CONSTRAINT CK_UtilityReadings_Period CHECK (TRY_CONVERT(date, BillingPeriod + '-01', 23) IS NOT NULL),
        CONSTRAINT CK_UtilityReadings_NonNegative CHECK (ReadingStart >= 0 AND ReadingEnd >= 0),
        CONSTRAINT CK_UtilityReadings_Sequence CHECK (ReadingEnd >= ReadingStart)
    );
END;
GO

IF OBJECT_ID(N'dbo.Invoices', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Invoices
    (
        InvoiceId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Invoices PRIMARY KEY,
        InvoiceCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_Invoices_InvoiceCode UNIQUE,
        ContractId BIGINT NOT NULL,
        RoomId BIGINT NOT NULL,
        PayerUserId BIGINT NOT NULL,
        BillingPeriod CHAR(7) NOT NULL,
        IssueDate DATE NOT NULL,
        DueDate DATE NOT NULL,
        RentAmount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Invoices_RentAmount DEFAULT 0,
        UtilityAmount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Invoices_UtilityAmount DEFAULT 0,
        ServiceAmount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Invoices_ServiceAmount DEFAULT 0,
        AdjustmentAmount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Invoices_AdjustmentAmount DEFAULT 0,
        TotalAmount DECIMAL(19,2) NOT NULL,
        PaidAmount DECIMAL(19,2) NOT NULL CONSTRAINT DF_Invoices_PaidAmount DEFAULT 0,
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Invoices_Status DEFAULT 'DRAFT',
        Note NVARCHAR(2000) NULL,
        CreatedByUserId BIGINT NOT NULL,
        IssuedAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Invoices_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Invoices_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_Invoices_ContractPeriod UNIQUE (ContractId, BillingPeriod),
        CONSTRAINT FK_Invoices_Contract FOREIGN KEY (ContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_Invoices_Room FOREIGN KEY (RoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_Invoices_Payer FOREIGN KEY (PayerUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_Invoices_CreatedBy FOREIGN KEY (CreatedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_Invoices_Period CHECK (TRY_CONVERT(date, BillingPeriod + '-01', 23) IS NOT NULL),
        CONSTRAINT CK_Invoices_DateRange CHECK (DueDate >= IssueDate),
        CONSTRAINT CK_Invoices_Amounts CHECK (RentAmount >= 0 AND UtilityAmount >= 0 AND ServiceAmount >= 0 AND TotalAmount >= 0 AND PaidAmount >= 0 AND PaidAmount <= TotalAmount),
        CONSTRAINT CK_Invoices_Status CHECK (Status IN ('DRAFT', 'UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.InvoiceItems', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InvoiceItems
    (
        InvoiceItemId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_InvoiceItems PRIMARY KEY,
        InvoiceId BIGINT NOT NULL,
        ItemType VARCHAR(30) NOT NULL,
        Description NVARCHAR(500) NOT NULL,
        Quantity DECIMAL(18,3) NOT NULL,
        UnitName NVARCHAR(50) NOT NULL,
        UnitPrice DECIMAL(19,2) NOT NULL,
        Amount DECIMAL(19,2) NOT NULL,
        FeeConfigId BIGINT NULL,
        UtilityReadingId BIGINT NULL,
        ServiceId BIGINT NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_InvoiceItems_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_InvoiceItems_Invoice FOREIGN KEY (InvoiceId) REFERENCES dbo.Invoices(InvoiceId),
        CONSTRAINT FK_InvoiceItems_FeeConfig FOREIGN KEY (FeeConfigId) REFERENCES dbo.FeeConfigs(FeeConfigId),
        CONSTRAINT FK_InvoiceItems_Reading FOREIGN KEY (UtilityReadingId) REFERENCES dbo.UtilityReadings(UtilityReadingId),
        CONSTRAINT FK_InvoiceItems_Service FOREIGN KEY (ServiceId) REFERENCES dbo.Services(ServiceId),
        CONSTRAINT CK_InvoiceItems_Type CHECK (ItemType IN ('RENT', 'ELECTRICITY', 'WATER', 'SERVICE', 'ADJUSTMENT', 'OTHER')),
        CONSTRAINT CK_InvoiceItems_Amounts CHECK (Quantity >= 0 AND UnitPrice >= 0 AND Amount >= 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.Payments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payments
    (
        PaymentId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payments PRIMARY KEY,
        TransactionCode NVARCHAR(100) NOT NULL CONSTRAINT UQ_Payments_TransactionCode UNIQUE,
        InvoiceId BIGINT NOT NULL,
        PayerUserId BIGINT NOT NULL,
        ProviderCode VARCHAR(30) NOT NULL,
        PaymentMethod VARCHAR(30) NOT NULL,
        Amount DECIMAL(19,2) NOT NULL,
        CurrencyCode CHAR(3) NOT NULL CONSTRAINT DF_Payments_CurrencyCode DEFAULT 'VND',
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_Payments_Status DEFAULT 'PENDING',
        ProviderTransactionCode NVARCHAR(200) NULL,
        ExpiresAt DATETIME2(3) NULL,
        ConfirmedAt DATETIME2(3) NULL,
        ConfirmedByUserId BIGINT NULL,
        FailureReason NVARCHAR(1000) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Payments_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Payments_UpdatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Payments_Invoice FOREIGN KEY (InvoiceId) REFERENCES dbo.Invoices(InvoiceId),
        CONSTRAINT FK_Payments_Payer FOREIGN KEY (PayerUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_Payments_ConfirmedBy FOREIGN KEY (ConfirmedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_Payments_Amount CHECK (Amount > 0),
        CONSTRAINT CK_Payments_Status CHECK (Status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED', 'MANUAL_REVIEW'))
    );
END;
GO

IF OBJECT_ID(N'dbo.PaymentCallbacks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentCallbacks
    (
        PaymentCallbackId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PaymentCallbacks PRIMARY KEY,
        PaymentId BIGINT NULL,
        ProviderCode VARCHAR(30) NOT NULL,
        ProviderTransactionCode NVARCHAR(200) NULL,
        PayloadJson NVARCHAR(MAX) NOT NULL,
        SignatureValid BIT NOT NULL,
        ProcessingStatus VARCHAR(20) NOT NULL CONSTRAINT DF_PaymentCallbacks_ProcessingStatus DEFAULT 'RECEIVED',
        ProcessingMessage NVARCHAR(1000) NULL,
        ReceivedAt DATETIME2(3) NOT NULL CONSTRAINT DF_PaymentCallbacks_ReceivedAt DEFAULT SYSUTCDATETIME(),
        ProcessedAt DATETIME2(3) NULL,
        CONSTRAINT FK_PaymentCallbacks_Payment FOREIGN KEY (PaymentId) REFERENCES dbo.Payments(PaymentId),
        CONSTRAINT CK_PaymentCallbacks_Status CHECK (ProcessingStatus IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.Notifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications
    (
        NotificationId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notifications PRIMARY KEY,
        RecipientUserId BIGINT NOT NULL,
        NotificationType VARCHAR(40) NOT NULL,
        Title NVARCHAR(300) NOT NULL,
        Content NVARCHAR(MAX) NOT NULL,
        RelatedEntityType VARCHAR(50) NULL,
        RelatedEntityId BIGINT NULL,
        IsRead BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT 0,
        ReadAt DATETIME2(3) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Notifications_Recipient FOREIGN KEY (RecipientUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.MaintenanceRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MaintenanceRequests
    (
        MaintenanceRequestId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MaintenanceRequests PRIMARY KEY,
        RequestCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_MaintenanceRequests_RequestCode UNIQUE,
        ReporterUserId BIGINT NOT NULL,
        RoomId BIGINT NOT NULL,
        ContractId BIGINT NULL,
        IssueType VARCHAR(30) NOT NULL,
        Title NVARCHAR(300) NOT NULL,
        Description NVARCHAR(MAX) NOT NULL,
        Priority VARCHAR(20) NOT NULL CONSTRAINT DF_MaintenanceRequests_Priority DEFAULT 'NORMAL',
        Status VARCHAR(20) NOT NULL CONSTRAINT DF_MaintenanceRequests_Status DEFAULT 'OPEN',
        AssignedToUserId BIGINT NULL,
        EstimatedCost DECIMAL(19,2) NULL,
        ActualCost DECIMAL(19,2) NULL,
        ResolutionNote NVARCHAR(2000) NULL,
        RejectionReason NVARCHAR(2000) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_MaintenanceRequests_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_MaintenanceRequests_UpdatedAt DEFAULT SYSUTCDATETIME(),
        ResolvedAt DATETIME2(3) NULL,
        CONSTRAINT FK_MaintenanceRequests_Reporter FOREIGN KEY (ReporterUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_MaintenanceRequests_Room FOREIGN KEY (RoomId) REFERENCES dbo.Rooms(RoomId),
        CONSTRAINT FK_MaintenanceRequests_Contract FOREIGN KEY (ContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_MaintenanceRequests_AssignedTo FOREIGN KEY (AssignedToUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_MaintenanceRequests_IssueType CHECK (IssueType IN ('ELECTRICITY', 'WATER', 'EQUIPMENT', 'OTHER')),
        CONSTRAINT CK_MaintenanceRequests_Priority CHECK (Priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
        CONSTRAINT CK_MaintenanceRequests_Status CHECK (Status IN ('OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED', 'REJECTED')),
        CONSTRAINT CK_MaintenanceRequests_Cost CHECK ((EstimatedCost IS NULL OR EstimatedCost >= 0) AND (ActualCost IS NULL OR ActualCost >= 0))
    );
END;
GO

IF OBJECT_ID(N'dbo.MaintenanceRequestHistories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MaintenanceRequestHistories
    (
        MaintenanceRequestHistoryId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MaintenanceRequestHistories PRIMARY KEY,
        MaintenanceRequestId BIGINT NOT NULL,
        OldStatus VARCHAR(20) NULL,
        NewStatus VARCHAR(20) NOT NULL,
        Note NVARCHAR(2000) NULL,
        ChangedByUserId BIGINT NOT NULL,
        ChangedAt DATETIME2(3) NOT NULL CONSTRAINT DF_MaintenanceRequestHistories_ChangedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_MaintenanceRequestHistories_Request FOREIGN KEY (MaintenanceRequestId) REFERENCES dbo.MaintenanceRequests(MaintenanceRequestId),
        CONSTRAINT FK_MaintenanceRequestHistories_User FOREIGN KEY (ChangedByUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.FileAttachments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FileAttachments
    (
        FileAttachmentId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_FileAttachments PRIMARY KEY,
        MaintenanceRequestId BIGINT NULL,
        UploadedByUserId BIGINT NOT NULL,
        OriginalFileName NVARCHAR(255) NOT NULL,
        StoredFileName NVARCHAR(255) NOT NULL,
        StoragePath NVARCHAR(1000) NOT NULL,
        ContentType NVARCHAR(150) NOT NULL,
        FileSizeBytes BIGINT NOT NULL,
        FileHash NVARCHAR(128) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_FileAttachments_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_FileAttachments_Request FOREIGN KEY (MaintenanceRequestId) REFERENCES dbo.MaintenanceRequests(MaintenanceRequestId),
        CONSTRAINT FK_FileAttachments_UploadedBy FOREIGN KEY (UploadedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_FileAttachments_Size CHECK (FileSizeBytes > 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.ActivityLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ActivityLogs
    (
        ActivityLogId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ActivityLogs PRIMARY KEY,
        ActorUserId BIGINT NULL,
        ActionCode VARCHAR(60) NOT NULL,
        EntityType VARCHAR(60) NULL,
        EntityId BIGINT NULL,
        ResultCode VARCHAR(30) NOT NULL,
        BeforeData NVARCHAR(MAX) NULL,
        AfterData NVARCHAR(MAX) NULL,
        IpAddress VARCHAR(45) NULL,
        UserAgent NVARCHAR(500) NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_ActivityLogs_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_ActivityLogs_Actor FOREIGN KEY (ActorUserId) REFERENCES dbo.AppUsers(UserId)
    );
END;
GO

IF OBJECT_ID(N'dbo.ScheduledJobRuns', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ScheduledJobRuns
    (
        ScheduledJobRunId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ScheduledJobRuns PRIMARY KEY,
        JobCode VARCHAR(60) NOT NULL,
        BusinessKey NVARCHAR(200) NULL,
        StartedAt DATETIME2(3) NOT NULL,
        FinishedAt DATETIME2(3) NULL,
        Status VARCHAR(20) NOT NULL,
        SuccessCount INT NOT NULL CONSTRAINT DF_ScheduledJobRuns_SuccessCount DEFAULT 0,
        SkippedCount INT NOT NULL CONSTRAINT DF_ScheduledJobRuns_SkippedCount DEFAULT 0,
        ErrorCount INT NOT NULL CONSTRAINT DF_ScheduledJobRuns_ErrorCount DEFAULT 0,
        ErrorMessage NVARCHAR(2000) NULL,
        CONSTRAINT CK_ScheduledJobRuns_Status CHECK (Status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')),
        CONSTRAINT CK_ScheduledJobRuns_Counts CHECK (SuccessCount >= 0 AND SkippedCount >= 0 AND ErrorCount >= 0)
    );
END;
GO

/* Indexes supporting ownership checks, dashboards, filtering and scheduled jobs. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_AuthSessions_User_Expires' AND object_id = OBJECT_ID(N'dbo.AuthSessions'))
    CREATE INDEX IX_AuthSessions_User_Expires ON dbo.AuthSessions(UserId, ExpiresAt);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_RentalRequests_Applicant_Status' AND object_id = OBJECT_ID(N'dbo.RentalRequests'))
    CREATE INDEX IX_RentalRequests_Applicant_Status ON dbo.RentalRequests(ApplicantUserId, Status, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_RentalRequests_Room_Status' AND object_id = OBJECT_ID(N'dbo.RentalRequests'))
    CREATE INDEX IX_RentalRequests_Room_Status ON dbo.RentalRequests(RoomId, Status, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Contracts_Tenant_Status' AND object_id = OBJECT_ID(N'dbo.Contracts'))
    CREATE INDEX IX_Contracts_Tenant_Status ON dbo.Contracts(TenantUserId, Status, EndDate);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Contracts_Room_Status' AND object_id = OBJECT_ID(N'dbo.Contracts'))
    CREATE INDEX IX_Contracts_Room_Status ON dbo.Contracts(RoomId, Status, EndDate);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Invoices_Payer_Status_DueDate' AND object_id = OBJECT_ID(N'dbo.Invoices'))
    CREATE INDEX IX_Invoices_Payer_Status_DueDate ON dbo.Invoices(PayerUserId, Status, DueDate);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Payments_Invoice_Status' AND object_id = OBJECT_ID(N'dbo.Payments'))
    CREATE INDEX IX_Payments_Invoice_Status ON dbo.Payments(InvoiceId, Status, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Notifications_Recipient_Read' AND object_id = OBJECT_ID(N'dbo.Notifications'))
    CREATE INDEX IX_Notifications_Recipient_Read ON dbo.Notifications(RecipientUserId, IsRead, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_MaintenanceRequests_Reporter_Status' AND object_id = OBJECT_ID(N'dbo.MaintenanceRequests'))
    CREATE INDEX IX_MaintenanceRequests_Reporter_Status ON dbo.MaintenanceRequests(ReporterUserId, Status, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ActivityLogs_Entity_CreatedAt' AND object_id = OBJECT_ID(N'dbo.ActivityLogs'))
    CREATE INDEX IX_ActivityLogs_Entity_CreatedAt ON dbo.ActivityLogs(EntityType, EntityId, CreatedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_ScheduledJobRuns_Job_BusinessKey' AND object_id = OBJECT_ID(N'dbo.ScheduledJobRuns'))
    CREATE UNIQUE INDEX UX_ScheduledJobRuns_Job_BusinessKey ON dbo.ScheduledJobRuns(JobCode, BusinessKey) WHERE BusinessKey IS NOT NULL;
GO

/* Required fixed roles. */
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleCode = 'ADMIN')
    INSERT INTO dbo.Roles(RoleCode, RoleName) VALUES ('ADMIN', N'Quản trị viên');
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleCode = 'TENANT')
    INSERT INTO dbo.Roles(RoleCode, RoleName) VALUES ('TENANT', N'Người thuê');
GO

/* Default rules agreed for this project. Values can be changed through the backend admin functions. */
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'APP_NAME')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('APP_NAME', N'Quản lý nhà trọ', N'Tên hệ thống');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'CURRENCY')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('CURRENCY', N'VND', N'Đơn vị tiền tệ');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'TIME_ZONE')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('TIME_ZONE', N'Asia/Ho_Chi_Minh', N'Múi giờ hiển thị');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'DATE_FORMAT')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('DATE_FORMAT', N'dd/MM/yyyy', N'Định dạng ngày hiển thị');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'PRORATION_MODE')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('PRORATION_MODE', N'ACTUAL_DAYS', N'Tính tiền giữa tháng theo số ngày thực tế');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'SHARED_FEE_MODE')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('SHARED_FEE_MODE', N'PER_ROOM', N'Phí ở ghép tính cố định theo phòng');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'DEPOSIT_SETTLEMENT_MODE')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('DEPOSIT_SETTLEMENT_MODE', N'FULL_REFUND', N'Hoàn toàn bộ tiền cọc khi kết thúc thuê');
IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfigs WHERE ConfigKey = 'CONTRACT_EXPIRY_WARNING_DAYS')
    INSERT INTO dbo.SystemConfigs(ConfigKey, ConfigValue, Description) VALUES ('CONTRACT_EXPIRY_WARNING_DAYS', N'30', N'Số ngày cảnh báo hợp đồng sắp hết hạn');
GO

/* Optional development seed: set to 1 before running this section to create 30 vacant rooms. */
DECLARE @SeedDevelopmentData BIT = 0;
IF @SeedDevelopmentData = 1
BEGIN
    DECLARE @RoomNumber INT = 1;
    WHILE @RoomNumber <= 30
    BEGIN
        DECLARE @RoomCode NVARCHAR(50) = CONCAT(N'P', RIGHT(CONCAT('0', @RoomNumber), 2));
        IF NOT EXISTS (SELECT 1 FROM dbo.Rooms WHERE RoomCode = @RoomCode)
        BEGIN
            INSERT INTO dbo.Rooms(RoomCode, RoomName, FloorNumber, AreaM2, BaseRent, MaxOccupants, AllowsSharing, Status)
            VALUES (@RoomCode, CONCAT(N'Phòng ', @RoomNumber), ((@RoomNumber - 1) / 10) + 1, 20.00, 3000000.00, 2, 1, 'VACANT');
        END;
        SET @RoomNumber += 1;
    END;
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SchemaVersions WHERE VersionNumber = 1)
    INSERT INTO dbo.SchemaVersions(VersionNumber, Description) VALUES (1, N'Initial schema for QuanLyNhaTro');
GO

/* V2: electronic contract documents and online signing (same as migrations/V2__contract_documents.sql). */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'dbo.ContractDocuments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ContractDocuments
    (
        ContractDocumentId BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ContractDocuments PRIMARY KEY,
        ContractId BIGINT NOT NULL,
        DocumentType VARCHAR(20) NOT NULL,
        Status VARCHAR(20) NOT NULL,
        PreviousEndDate DATE NULL,
        NewEndDate DATE NULL,
        RenewalMonths INT NULL,
        ContentSnapshot NVARCHAR(MAX) NOT NULL,
        ContentHash VARCHAR(64) NOT NULL,
        SignerUserId BIGINT NOT NULL,
        SignedAt DATETIME2(3) NULL,
        SignerIp NVARCHAR(64) NULL,
        SignerUserAgent NVARCHAR(500) NULL,
        SignedPdfPath NVARCHAR(1000) NULL,
        SignedPdfHash VARCHAR(64) NULL,
        CreatedByUserId BIGINT NOT NULL,
        CreatedAt DATETIME2(3) NOT NULL CONSTRAINT DF_ContractDocuments_CreatedAt DEFAULT SYSUTCDATETIME(),
        VoidedAt DATETIME2(3) NULL,
        CONSTRAINT FK_ContractDocuments_Contract FOREIGN KEY (ContractId) REFERENCES dbo.Contracts(ContractId),
        CONSTRAINT FK_ContractDocuments_Signer FOREIGN KEY (SignerUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT FK_ContractDocuments_CreatedBy FOREIGN KEY (CreatedByUserId) REFERENCES dbo.AppUsers(UserId),
        CONSTRAINT CK_ContractDocuments_Type CHECK (DocumentType IN ('CONTRACT', 'RENEWAL')),
        CONSTRAINT CK_ContractDocuments_Status CHECK (Status IN ('AWAITING_SIGNATURE', 'SIGNED', 'VOID')),
        CONSTRAINT CK_ContractDocuments_RenewalMonths CHECK (RenewalMonths IS NULL OR RenewalMonths BETWEEN 1 AND 36),
        CONSTRAINT CK_ContractDocuments_Renewal CHECK (
            DocumentType <> 'RENEWAL'
            OR (PreviousEndDate IS NOT NULL AND NewEndDate > PreviousEndDate AND RenewalMonths IS NOT NULL)),
        CONSTRAINT CK_ContractDocuments_Signed CHECK (
            Status <> 'SIGNED'
            OR (SignedAt IS NOT NULL AND SignedPdfPath IS NOT NULL AND SignedPdfHash IS NOT NULL))
    );
END;
GO

/* At most one document per contract can be waiting for a signature at any time. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_ContractDocuments_Awaiting' AND object_id = OBJECT_ID(N'dbo.ContractDocuments'))
    CREATE UNIQUE INDEX UX_ContractDocuments_Awaiting ON dbo.ContractDocuments(ContractId) WHERE Status = 'AWAITING_SIGNATURE';
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ContractDocuments_Contract_Created' AND object_id = OBJECT_ID(N'dbo.ContractDocuments'))
    CREATE INDEX IX_ContractDocuments_Contract_Created ON dbo.ContractDocuments(ContractId, CreatedAt);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SchemaVersions WHERE VersionNumber = 2)
    INSERT INTO dbo.SchemaVersions(VersionNumber, Description) VALUES (2, N'Electronic contract documents and online signing');
GO

/* Quick verification output. */
SELECT DB_NAME() AS DatabaseName,
       (SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped = 0) AS UserTableCount,
       (SELECT COUNT(*) FROM dbo.Roles) AS RoleCount;
GO
