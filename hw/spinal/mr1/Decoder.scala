package mr1

import spinal.core._



case class DecodedInstr(config: MR1Config) extends Bundle {

    val iformat = InstrFormat()
    val itype = InstrType()

}

case class Decode2Execute(config: MR1Config) extends Bundle {

    val valid = Bool 
    val decoded_instr = DecodedInstr(config)
    
    val rvfi = if (config.hasFormal) RVFI(config) else null
}

case class Execute2Decode(config: MR1Config) extends Bundle {

    val stall = Bool
}

case class Decoder(config: MR1Config) extends Component {

    val hasMul = config.hasMul 
    val hasDiv = config.hasMul 
    val hasCsr = config.hasMul 
    val hasFence = config.hasFence
    
    val io = new Bundle {
        val f2d = in(Fetch2Decode(config))
        val d2f = in(Decode2Fetch(config))
        

        val d2e = in(Decode2Execute(config))
        val e2d = in(Execute2Decode(config))
    }
    
    val decode = new Area {

        val instr = io.f2d.instr        
         
        val opCode = instr(6 downto 0)
        val funct3 = instr(14 downto 12)
        val funct7 = instr(31 downto 25)
        val rd = instr(11 downto 7)
        val rs1 = instr(19 downto 15)
        val rs2 = instr(24 downto 20)
        

        val i_imm_11_0 = instr(31 downto 20)
        val s_imm_11_0 = instr(31 downto 25) ## instr(11 downto 7)
        val b_imm_12_0 = instr(31) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8)
        val u_imm_31_12 = instr(31 downto 12)
        val j_imm_20_12 = instr(20) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)


        val decoded_instr = DecodedInstr(config)
        
        decoded_instr.iformat := InstrFormat.Undef 
        decoded_instr.itype   := InstrType.LUI
        
        switch(opCode){
           // LUI
            is(B"0110111"){
                decoded_instr.itype     := InstrType.LUI
                decoded_instr.iformat   := InstrFormat.U
            }
            // AUIPC
            is(B"0010111"){
                decoded_instr.itype     := InstrType.AUIPC
                decoded_instr.iformat   := InstrFormat.U
            }
            // JAL
            is(B"1101111"){
                decoded_instr.itype     := InstrType.JAL
                decoded_instr.iformat   := InstrFormat.J
            }
            // JALR
            is(B"1100111"){
                when(funct3 === B"000") {
                    decoded_instr.itype     := InstrType.JALR
                    decoded_instr.iformat   := InstrFormat.I
                }
            }
            // Bxx
            is(B"1100011"){
                when(funct3 =/= B"010" && funct3 =/= B"011") {
                    decoded_instr.itype     := InstrType.B
                    decoded_instr.iformat   := InstrFormat.B
                }
            }
            // Lxx
            is(B"0000011"){
                when(funct3 =/= B"010" && funct3 =/= B"011" && funct3 =/= B"110" && funct3 =/= B"111") {
                    decoded_instr.itype     := InstrType.L
                    decoded_instr.iformat   := InstrFormat.I
                }
            }
            // Sx
            is(B"0100011"){
                when(funct3 === B"000" || funct3 === B"001" || funct3 === B"010") {
                    decoded_instr.itype     := InstrType.S
                    decoded_instr.iformat   := InstrFormat.S
                }
            }
            is(B"0010011"){
                when(funct3 === B"000" || funct3 === B"010" || funct3 === B"011" || funct3 === B"100" || funct3 === B"110" || funct3 === B"111") {
                    // ALU_I
                    decoded_instr.itype     := InstrType.ALU_I
                    decoded_instr.iformat   := InstrFormat.I
                }.elsewhen( (funct7 ## funct3) === B"0000000001" || (funct7 ## funct3) === B"0000000101" || (funct7 ## funct3) === B"0100000101") {
                    // SHIFT_I
                    decoded_instr.itype     := InstrType.SHIFT_I
                    decoded_instr.iformat   := InstrFormat.R
                }
            }
            // ALU, SHIFT
            is(B"0110011"){
                switch(funct7 ## funct3){
                    is(B"0000000_000", B"0100000_000", B"0000000_100", B"0000000_110", B"0000000_111"){
                        // ADD, SUB, XOR, OR, AND
                        decoded_instr.itype     := InstrType.ALU
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is(B"0000000_001", B"0000000_101", B"0100000_101"){
                        // SLL, SRL, SRA
                        decoded_instr.itype     := InstrType.SHIFT
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is( B"0000000_010", B"0000000_011") {
                        // SLT, SLTU
                        decoded_instr.itype     := InstrType.ALU
                        decoded_instr.iformat   := InstrFormat.R
                    }
                    is(B"0000001_000", B"0000001_001", B"0000001_010", B"0000001_011"){
                        // MUL
                        if (hasMul){
                            when(funct7 === B"0000001"){
                                decoded_instr.itype     := InstrType.MUL
                                decoded_instr.iformat   := InstrFormat.R
                            }
                        }
                    }
                    is(B"0000001_100", B"0000001_101", B"0000001_110", B"0000001_111"){
                        // DIV
                        if (hasDiv){
                            when(funct7 === B"0000001"){
                                decoded_instr.itype     := InstrType.DIV
                                decoded_instr.iformat   := InstrFormat.R
                            }
                        }
                    }
                }
            }
            // FENCE
            is(B"0001111"){
                if (hasFence){
                    when( funct3 === B"000" || funct3 === B"001"){
                        decoded_instr.itype     := InstrType.FENCE
                        decoded_instr.iformat   := InstrFormat.I
                    }
                }
            }
            // ECALL, EBREAK, CSR
            is(B"1110011"){
                when( instr(31 downto 7) === B"0000_0000_0000_0000_0000_0000_0" || instr(31 downto 7) === B"0000_0000_0001_0000_0000_0000_0")
                {
                    decoded_instr.itype     := InstrType.E
                    decoded_instr.iformat   := InstrFormat.I
                }.elsewhen(funct3 === B"001" || funct3 === B"010" || funct3 === B"011" || funct3 === B"101" || funct3 === B"110" || funct3 === B"111") {
                    if (hasCsr){
                        decoded_instr.itype     := InstrType.CSR
                        decoded_instr.iformat   := InstrFormat.I
                    }
                }
            }
        }

        
    }

    val outputStage = new Area {
        io.d2e.valid := RegNext(io.f2d.valid)
        io.d2e.decoded_instr := RegNextWhen(decode.decoded_instr, io.f2d.valid && !io.e2d.stall).setName("d2e_decoded_instr")

    } 
    
    io.d2f.stall := False 

    val formal = if (config.hasFormal) new Area {

        val rvfi = RVFI(config)
        
        val order = Reg(UInt(64 bits)) init(0)
        
        when(io.f2d.valid) {
            order := order +1
        }
             rvfi.valid      := io.f2d.valid
        rvfi.order      := order
        rvfi.insn       := io.f2d.instr
        rvfi.trap       := (decode.decoded_instr.iformat === InstrFormat.Undef)
        rvfi.halt       := False
        rvfi.intr       := False
        rvfi.rs1_addr   := decode.rs1
        rvfi.rs2_addr   := decode.rs2
        rvfi.rs1_rdata  := 0
        rvfi.rs2_rdata  := 0
        rvfi.rd_addr    := decode.rd
        rvfi.rd_wdata   := 0
        rvfi.pc_rdata   := 0
        rvfi.pc_wdata   := 0
        rvfi.mem_addr   := 0
        rvfi.mem_rmask  := 0
        rvfi.mem_wmask  := 0
        rvfi.mem_rdata  := 0
        rvfi.mem_wdata  := 0

        io.d2e.rvfi := RegNext(rvfi).setName("d2e_rvfi") init()
    }
    

    
}          