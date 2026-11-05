/*
    QuanLyNhaTro - Migration V2: electronic contract documents and online signing.

    Safety:
    - Additive only: creates dbo.ContractDocuments when it does not exist.
    - Does not DROP, TRUNCATE, ALTER or delete existing data; safe to run more than once.
    - The same block is included in database.sql for fresh installs.
*/

USE [QuanLyNhaTro];
GO

/* Filtered indexes require these options; sqlcmd turns QUOTED_IDENTIFIER off by default. */
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
